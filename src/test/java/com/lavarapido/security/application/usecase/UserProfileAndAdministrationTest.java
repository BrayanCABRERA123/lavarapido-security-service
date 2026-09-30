package com.lavarapido.security.application.usecase;

import com.lavarapido.security.application.fake.Fakes.FakeTokenIssuer;
import com.lavarapido.security.application.fake.Fakes.InMemoryUserSessionRepository;
import com.lavarapido.security.application.fake.Fakes.MutableClock;
import com.lavarapido.security.application.fake.Fakes.PlainPasswordHasher;
import com.lavarapido.security.application.fake.Fakes.RecordingEventPublisher;
import com.lavarapido.security.application.fake.InMemoryUserAccountRepository;
import com.lavarapido.security.domain.event.AccountDeactivated;
import com.lavarapido.security.domain.event.PasswordChanged;
import com.lavarapido.security.domain.exception.AccountDisabledException;
import com.lavarapido.security.domain.exception.CannotDisableOwnAccountException;
import com.lavarapido.security.domain.exception.EmailAlreadyRegisteredException;
import com.lavarapido.security.domain.exception.InvalidCredentialsException;
import com.lavarapido.security.domain.exception.IncorrectCurrentPasswordException;
import com.lavarapido.security.domain.exception.SamePasswordException;
import com.lavarapido.security.domain.exception.UserNotFoundException;
import com.lavarapido.security.domain.model.PageResult;
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
    private final AuthenticationService authentication =
            new AuthenticationService(accounts, sessions, hasher, new FakeTokenIssuer(), clock);
    private final AccountRegistrationService registration =
            new AccountRegistrationService(accounts, hasher, new PasswordPolicy(), new RecordingEventPublisher(), clock);

    private long anaId;

    @BeforeEach
    void registerAna() {
        anaId = registration.register(new RegisterUserCommand("1023456789", "Ana", "Pérez", "ana@gmail.com",
                "3001234567", "Lavado2026!")).id();
    }

    @Nested
    class Profile {

        private final UserProfileService service =
                new UserProfileService(accounts, hasher, new PasswordPolicy(), events, clock);

        @Test
        void updatesNameAndPhoneButNotTheLogin() {
            UserAccountView view = service.updateProfile(new UpdateProfileCommand(anaId, "Ana María", "Pérez", null));

            assertThat(view.firstName()).isEqualTo("Ana María");
            assertThat(view.phone()).isNull();
            assertThat(view.email()).isEqualTo("ana@gmail.com");
        }

        @Test
        void changingThePasswordRequiresTheCurrentOne() {
            assertThatThrownBy(() -> service.changePassword(new ChangePasswordCommand(anaId, "Wrong2026!", "Nueva2026!")))
                    .isInstanceOf(IncorrectCurrentPasswordException.class);
        }

        @Test
        void theNewPasswordMustDifferFromTheCurrentOne() {
            assertThatThrownBy(() -> service.changePassword(new ChangePasswordCommand(anaId, "Lavado2026!", "Lavado2026!")))
                    .isInstanceOf(SamePasswordException.class);
        }

        @Test
        void changesThePasswordAndPublishesTheEvent() {
            service.changePassword(new ChangePasswordCommand(anaId, "Lavado2026!", "Nueva2026!"));

            assertThat(hasher.matches("Nueva2026!", accounts.findById(anaId).orElseThrow().passwordHash())).isTrue();
            assertThat(events.events).singleElement().isInstanceOf(PasswordChanged.class);
        }

        @Test
        void unknownUserIsNotFound() {
            assertThatThrownBy(() -> service.getProfile(999)).isInstanceOf(UserNotFoundException.class);
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
        void anAdministratorCannotDisableThemselves() {
            assertThatThrownBy(() -> service.changeStatus(anaId, false, anaId))
                    .isInstanceOf(CannotDisableOwnAccountException.class);
        }
    }

    @Nested
    class Deactivation {

        private final AccountDeactivationService service =
                new AccountDeactivationService(accounts, sessions, hasher, events, clock);

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
        void theAccountIsKeptNotDeleted() {
            service.deactivateOwnAccount(new DeactivateOwnAccountCommand(anaId, "Lavado2026!"));

            assertThat(accounts.findById(anaId)).isPresent();
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

        private final EmailChangeService service = new EmailChangeService(accounts, hasher, events, clock);

        @Test
        void requiresTheCurrentPassword() {
            assertThatThrownBy(() -> service.changeEmail(new ChangeEmailCommand(anaId, "ana.nueva@gmail.com", "Wrong2026!")))
                    .isInstanceOf(IncorrectCurrentPasswordException.class);
        }

        @Test
        void cannotTakeAnEmailThatBelongsToSomeoneElse() {
            registration.register(new RegisterUserCommand("80000001", "Luis", "Gómez", "luis@gmail.com", null,
                    "Lavado2026!"));

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
            new UserProfileService(accounts, hasher, new PasswordPolicy(), events, clock)
                    .changePassword(new ChangePasswordCommand(anaId, "Lavado2026!", "Nueva2026!"));

            assertThat(authentication.login(new LoginCommand("ana@gmail.com", "Nueva2026!", null, null)).user().id())
                    .isEqualTo(anaId);
            assertThatThrownBy(() -> authentication.login(new LoginCommand("ana@gmail.com", "Lavado2026!", null, null)))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
    }
}
