package com.lavarapido.security.application.usecase;

import com.lavarapido.security.application.fake.Fakes.MutableClock;
import com.lavarapido.security.application.fake.Fakes.PlainPasswordHasher;
import com.lavarapido.security.application.fake.Fakes.RecordingEventPublisher;
import com.lavarapido.security.application.fake.InMemoryUserAccountRepository;
import com.lavarapido.security.domain.event.PasswordChanged;
import com.lavarapido.security.domain.exception.CannotDisableOwnAccountException;
import com.lavarapido.security.domain.exception.IncorrectCurrentPasswordException;
import com.lavarapido.security.domain.exception.SamePasswordException;
import com.lavarapido.security.domain.exception.UserNotFoundException;
import com.lavarapido.security.domain.model.PageResult;
import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.port.in.ChangePasswordUseCase.ChangePasswordCommand;
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

        private final UserAdministrationService service = new UserAdministrationService(accounts);

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
}
