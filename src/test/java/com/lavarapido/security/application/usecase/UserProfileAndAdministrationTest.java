package com.lavarapido.security.application.usecase;

import com.lavarapido.security.domain.model.AllowedEmailDomains;
import com.lavarapido.security.application.fake.Fakes.FakeTokenIssuer;
import com.lavarapido.security.application.fake.Fakes.InMemoryUserSessionRepository;
import com.lavarapido.security.application.fake.Fakes.MutableClock;
import com.lavarapido.security.application.fake.Fakes.PlainPasswordHasher;
import com.lavarapido.security.application.fake.Fakes.RecordingEventPublisher;
import com.lavarapido.security.application.fake.InMemoryUserAccountRepository;
import com.lavarapido.security.domain.event.AccountDeactivated;
import com.lavarapido.security.domain.event.PasswordChanged;
import com.lavarapido.security.domain.exception.AccountDisabledException;
import com.lavarapido.security.domain.exception.AccountLockedException;
import com.lavarapido.security.domain.exception.AdminAccountCannotBeDisabledException;
import com.lavarapido.security.domain.exception.CannotDisableOwnAccountException;
import com.lavarapido.security.domain.exception.EmailAlreadyRegisteredException;
import com.lavarapido.security.domain.exception.InvalidCredentialsException;
import com.lavarapido.security.domain.exception.IncorrectCurrentPasswordException;
import com.lavarapido.security.domain.exception.SamePasswordException;
import com.lavarapido.security.domain.exception.TooManyAttemptsException;
import com.lavarapido.security.domain.exception.UserNotFoundException;
import com.lavarapido.security.domain.model.PageResult;
import com.lavarapido.security.domain.exception.InvalidValueException;
import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.port.in.AuthenticateUserUseCase.LoginCommand;
import com.lavarapido.security.domain.port.in.ChangeEmailUseCase.ChangeEmailCommand;
import com.lavarapido.security.domain.port.in.ChangePasswordUseCase.ChangePasswordCommand;
import com.lavarapido.security.domain.port.in.DeactivateOwnAccountUseCase.DeactivateOwnAccountCommand;
import com.lavarapido.security.domain.port.in.CreateUserAccountUseCase.CreateUserAccountCommand;
import com.lavarapido.security.domain.port.in.ListUserAccountsUseCase.ListUserAccountsQuery;
import com.lavarapido.security.domain.port.in.RegisterUserUseCase.RegisterUserCommand;
import com.lavarapido.security.domain.port.in.UpdateUserProfileUseCase.UpdateProfileCommand;
import com.lavarapido.security.domain.port.in.UserAccountView;
import com.lavarapido.security.domain.service.PasswordPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserProfileAndAdministrationTest {

    private final InMemoryUserAccountRepository accounts = new InMemoryUserAccountRepository();
    private final PlainPasswordHasher hasher = new PlainPasswordHasher();
    private final RecordingEventPublisher events = new RecordingEventPublisher();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-09-29T15:00:00Z"));
    private final InMemoryUserSessionRepository sessions = new InMemoryUserSessionRepository();
    // el mismo bloqueo para el login y para la contraseña actual del perfil (SEC-12)
    private final com.lavarapido.security.application.fake.Fakes.InMemoryLoginLockoutRepository lockouts =
            new com.lavarapido.security.application.fake.Fakes.InMemoryLoginLockoutRepository();
    private final CurrentPasswordVerifier currentPassword = new CurrentPasswordVerifier(hasher, lockouts, clock);
    private final AuthenticationService authentication =
            new AuthenticationService(accounts, sessions, hasher, new FakeTokenIssuer(), lockouts, clock);
    private final AccountRegistrationService registration =
            new AccountRegistrationService(accounts, hasher, new PasswordPolicy(), new RecordingEventPublisher(), clock, AllowedEmailDomains.parse("gmail.com"));

    private long anaId;

    @BeforeEach
    void registerAna() {
        anaId = registration.register(new RegisterUserCommand("1023456789", "Ana", "Pérez", "ana@gmail.com",
                "3001234567", "Lavado2026!", true, true)).id();
    }

    @Nested
    class Profile {

        private final UserProfileService service =
                new UserProfileService(accounts, sessions, hasher, new PasswordPolicy(), events, clock, currentPassword);

        @Test
        void updatesNameAndPhoneButNotTheLogin() {
            UserAccountView view = service.updateProfile(new UpdateProfileCommand(anaId, "Ana María", "Pérez", null));

            assertThat(view.firstName()).isEqualTo("Ana María");
            assertThat(view.phone()).isNull();
            assertThat(view.email()).isEqualTo("ana@gmail.com");
        }

        @Test
        void changingThePasswordRequiresTheCurrentOne() {
            assertThatThrownBy(() -> service.changePassword(new ChangePasswordCommand(anaId, 0L, "Wrong2026!", "Nueva2026!")))
                    .isInstanceOf(IncorrectCurrentPasswordException.class);
        }

        @Test
        void theNewPasswordMustDifferFromTheCurrentOne() {
            assertThatThrownBy(() -> service.changePassword(new ChangePasswordCommand(anaId, 0L, "Lavado2026!", "Lavado2026!")))
                    .isInstanceOf(SamePasswordException.class);
        }

        @Test
        void changesThePasswordAndPublishesTheEvent() {
            service.changePassword(new ChangePasswordCommand(anaId, 0L, "Lavado2026!", "Nueva2026!"));

            assertThat(hasher.matches("Nueva2026!", accounts.findById(anaId).orElseThrow().passwordHash())).isTrue();
            assertThat(events.events).singleElement().isInstanceOf(PasswordChanged.class);
        }

        @Test
        void changingThePasswordClosesTheOtherSessionsButNotThisOne() {
            authentication.login(new LoginCommand("ana@gmail.com", "Lavado2026!", null, null));
            authentication.login(new LoginCommand("ana@gmail.com", "Lavado2026!", null, null));
            long thisSession = 1L;
            long otherSession = 2L;

            service.changePassword(new ChangePasswordCommand(anaId, thisSession, "Lavado2026!", "Nueva2026!"));

            assertThat(sessions.isActive(thisSession, anaId, clock.instant())).isTrue();
            assertThat(sessions.isActive(otherSession, anaId, clock.instant())).isFalse();
        }

        @Test
        void unknownUserIsNotFound() {
            assertThatThrownBy(() -> service.getProfile(999)).isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void fiveWrongCurrentPasswordsLockTheChangeAndTheLogin() {
            // SEC-12: sin tope, quien encontrara la sesión abierta podía adivinar la contraseña sin límite
            for (int attempt = 1; attempt < 5; attempt++) {
                assertThatThrownBy(() -> service.changePassword(new ChangePasswordCommand(anaId, 0L, "Wrong2026!", "Nueva2026!")))
                        .isInstanceOf(IncorrectCurrentPasswordException.class);
            }
            assertThatThrownBy(() -> service.changePassword(new ChangePasswordCommand(anaId, 0L, "Wrong2026!", "Nueva2026!")))
                    .isInstanceOf(TooManyAttemptsException.class).extracting("code").isEqualTo("TOO_MANY_ATTEMPTS");

            // bloqueada: ni la contraseña correcta sirve, tampoco para entrar
            assertThatThrownBy(() -> service.changePassword(new ChangePasswordCommand(anaId, 0L, "Lavado2026!", "Nueva2026!")))
                    .isInstanceOf(TooManyAttemptsException.class);
            assertThatThrownBy(() -> authentication.login(new LoginCommand("ana@gmail.com", "Lavado2026!", null, null)))
                    .isInstanceOf(AccountLockedException.class);
            assertThat(hasher.matches("Lavado2026!", accounts.findById(anaId).orElseThrow().passwordHash())).isTrue();

            // a los 15 minutos vuelve a poder
            clock.advance(java.time.Duration.ofMinutes(16));
            service.changePassword(new ChangePasswordCommand(anaId, 0L, "Lavado2026!", "Nueva2026!"));
            assertThat(lockouts.unlockedReads).isZero();
        }

        @Test
        void theRightCurrentPasswordClearsThePreviousMistakes() {
            for (int attempt = 0; attempt < 4; attempt++) {
                assertThatThrownBy(() -> service.changePassword(new ChangePasswordCommand(anaId, 0L, "Wrong2026!", "Nueva2026!")))
                        .isInstanceOf(IncorrectCurrentPasswordException.class);
            }
            service.changePassword(new ChangePasswordCommand(anaId, 0L, "Lavado2026!", "Nueva2026!"));

            assertThat(lockouts.rows).doesNotContainKey(anaId);
        }
    }

    @Nested
    class Administration {

        private final UserAdministrationService service = new UserAdministrationService(accounts, sessions, events, clock);

        @Test
        void listsAccountsFilteredByRole() {
            registration.createAccount(new CreateUserAccountCommand("80000001", "Luis", "Gómez", "luis@gmail.com",
                    null, "Operario2026!", Set.of(RoleCode.OPERATOR)));

            PageResult<UserAccountView> operators = service.listAccounts(new ListUserAccountsQuery(RoleCode.OPERATOR, 0, 20));

            assertThat(operators.items()).extracting(UserAccountView::email).containsExactly("luis@gmail.com");
            assertThat(operators.totalElements()).isEqualTo(1);
        }

        @Test
        void clampsAbsurdPageSizes() {
            PageResult<UserAccountView> page = service.listAccounts(new ListUserAccountsQuery(null, -3, 10_000));

            assertThat(page.page()).isZero();
            assertThat(page.size()).isEqualTo(UserAdministrationService.MAX_PAGE_SIZE);
        }

        @Test
        void disablesAndReEnablesAnAccount() {
            assertThat(service.changeStatus(anaId, false, 500).active()).isFalse();
            assertThat(service.changeStatus(anaId, true, 500).active()).isTrue();
        }

        @Test
        void anAdministratorAccountCannotBeDisabled() {
            long adminId = registration.createAccount(new CreateUserAccountCommand("80000003", "Luis", "Gómez",
                    "luis@gmail.com", null, "Admin2026!", Set.of(RoleCode.ADMIN))).id();

            assertThatThrownBy(() -> service.changeStatus(adminId, false, 500))
                    .isInstanceOf(AdminAccountCannotBeDisabledException.class);
            assertThat(accounts.findById(adminId).orElseThrow().isActive()).isTrue();
        }

        @Test
        void anAdministratorCannotDisableThemselves() {
            assertThatThrownBy(() -> service.changeStatus(anaId, false, anaId))
                    .isInstanceOf(CannotDisableOwnAccountException.class);
        }
    }

    @Nested
    class Deactivation {

        private final AccountDeactivationService service =
                new AccountDeactivationService(accounts, sessions, currentPassword, events, clock);

        private void login() {
            authentication.login(new LoginCommand("ana@gmail.com", "Lavado2026!", null, null));
        }

        @Test
        void closingTheAccountRequiresTheCurrentPassword() {
            assertThatThrownBy(() -> service.deactivateOwnAccount(new DeactivateOwnAccountCommand(anaId, "Wrong2026!")))
                    .isInstanceOf(IncorrectCurrentPasswordException.class);
            assertThat(accounts.findById(anaId).orElseThrow().isActive()).isTrue();
        }

        @Test
        void aClosedAccountCannotLogInAgainAndLosesItsSessions() {
            login();
            login();

            service.deactivateOwnAccount(new DeactivateOwnAccountCommand(anaId, "Lavado2026!"));

            assertThat(sessions.openSessionsOf(anaId)).isZero();
            assertThatThrownBy(this::login).isInstanceOf(AccountDisabledException.class);
            assertThat(events.events).singleElement().isInstanceOf(AccountDeactivated.class);
        }

        @Test
        void anAdministratorCannotCloseTheirOwnAccount() {
            long adminId = registration.createAccount(new CreateUserAccountCommand("80000002", "Sara", "Ruiz",
                    "sara@gmail.com", null, "Admin2026!", Set.of(RoleCode.ADMIN))).id();

            assertThatThrownBy(() -> service.deactivateOwnAccount(new DeactivateOwnAccountCommand(adminId, "Admin2026!")))
                    .isInstanceOf(CannotDisableOwnAccountException.class);
            assertThat(accounts.findById(adminId).orElseThrow().isActive()).isTrue();
        }

        @Test
        void theAccountIsKeptNotDeleted() {
            service.deactivateOwnAccount(new DeactivateOwnAccountCommand(anaId, "Lavado2026!"));

            assertThat(accounts.findById(anaId)).isPresent();
        }

        @Test
        void wrongPasswordsAreCountedTogetherWithTheOtherProfileActions() {
            // SEC-12: el tope es de la cuenta, no de cada pantalla
            UserProfileService profile =
                    new UserProfileService(accounts, sessions, hasher, new PasswordPolicy(), events, clock, currentPassword);
            EmailChangeService email =
                    new EmailChangeService(accounts, currentPassword, events, clock, AllowedEmailDomains.parse("gmail.com"));
            for (int attempt = 0; attempt < 2; attempt++) {
                assertThatThrownBy(() -> profile.changePassword(new ChangePasswordCommand(anaId, 0L, "Wrong2026!", "Nueva2026!")))
                        .isInstanceOf(IncorrectCurrentPasswordException.class);
                assertThatThrownBy(() -> email.changeEmail(new ChangeEmailCommand(anaId, "ana.nueva@gmail.com", "Wrong2026!")))
                        .isInstanceOf(IncorrectCurrentPasswordException.class);
            }

            assertThatThrownBy(() -> service.deactivateOwnAccount(new DeactivateOwnAccountCommand(anaId, "Wrong2026!")))
                    .isInstanceOf(TooManyAttemptsException.class);
            assertThatThrownBy(() -> service.deactivateOwnAccount(new DeactivateOwnAccountCommand(anaId, "Lavado2026!")))
                    .isInstanceOf(TooManyAttemptsException.class);
            assertThat(accounts.findById(anaId).orElseThrow().isActive()).isTrue();
            assertThat(accounts.findById(anaId).orElseThrow().username().value()).isEqualTo("ana@gmail.com");
        }

        @Test
        void anAdministratorDisablingSomeoneAlsoClosesTheirSessions() {
            login();

            new UserAdministrationService(accounts, sessions, events, clock).changeStatus(anaId, false, 500);

            assertThat(sessions.openSessionsOf(anaId)).isZero();
            assertThat(events.events).singleElement().isInstanceOf(AccountDeactivated.class);
        }
    }

    @Nested
    class EmailChange {

        private final EmailChangeService service = new EmailChangeService(accounts, currentPassword, events, clock, AllowedEmailDomains.parse("gmail.com"));

        @Test
        void requiresTheCurrentPassword() {
            assertThatThrownBy(() -> service.changeEmail(new ChangeEmailCommand(anaId, "ana.nueva@gmail.com", "Wrong2026!")))
                    .isInstanceOf(IncorrectCurrentPasswordException.class);
        }

        @Test
        void theNewEmailMustBelongToAnAllowedDomain() {
            // A-01
            assertThatThrownBy(() -> service.changeEmail(new ChangeEmailCommand(anaId, "ana@hotmail.com", "Lavado2026!")))
                    .isInstanceOf(InvalidValueException.class).extracting("code").isEqualTo("EMAIL_DOMAIN_NOT_ALLOWED");
        }

        @Test
        void cannotTakeAnEmailThatBelongsToSomeoneElse() {
            registration.register(new RegisterUserCommand("80000001", "Luis", "Gómez", "luis@gmail.com", null,
                    "Lavado2026!", true, true));

            assertThatThrownBy(() -> service.changeEmail(new ChangeEmailCommand(anaId, "luis@gmail.com", "Lavado2026!")))
                    .isInstanceOf(EmailAlreadyRegisteredException.class);
        }

        @Test
        void afterTheChangeTheNewEmailLogsInAndTheOldOneDoesNot() {
            UserAccountView view = service.changeEmail(new ChangeEmailCommand(anaId, "Ana.Nueva@Gmail.com", "Lavado2026!"));

            assertThat(view.email()).isEqualTo("ana.nueva@gmail.com");
            assertThat(authentication.login(new LoginCommand("ana.nueva@gmail.com", "Lavado2026!", null, null))
                    .user().id()).isEqualTo(anaId);
            assertThatThrownBy(() -> authentication.login(new LoginCommand("ana@gmail.com", "Lavado2026!", null, null)))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        void aPasswordChangedFromTheProfileIsTheOneThatLogsIn() {
            new UserProfileService(accounts, sessions, hasher, new PasswordPolicy(), events, clock, currentPassword)
                    .changePassword(new ChangePasswordCommand(anaId, 0L, "Lavado2026!", "Nueva2026!"));

            assertThat(authentication.login(new LoginCommand("ana@gmail.com", "Nueva2026!", null, null)).user().id())
                    .isEqualTo(anaId);
            assertThatThrownBy(() -> authentication.login(new LoginCommand("ana@gmail.com", "Lavado2026!", null, null)))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
    }
}
