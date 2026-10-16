package com.lavarapido.security.application.usecase;

import com.lavarapido.security.application.fake.Fakes.MutableClock;
import com.lavarapido.security.application.fake.Fakes.PlainPasswordHasher;
import com.lavarapido.security.application.fake.Fakes.RecordingEventPublisher;
import com.lavarapido.security.application.fake.InMemoryUserAccountRepository;
import com.lavarapido.security.domain.exception.WeakPasswordException;
import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.port.in.CreateUserAccountUseCase.CreateUserAccountCommand;
import com.lavarapido.security.domain.port.in.UserAccountView;
import com.lavarapido.security.domain.service.PasswordPolicy;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdministratorBootstrapServiceTest {

    private final InMemoryUserAccountRepository accounts = new InMemoryUserAccountRepository();
    private final RecordingEventPublisher events = new RecordingEventPublisher();
    private final AccountRegistrationService registration = new AccountRegistrationService(accounts,
            new PlainPasswordHasher(), new PasswordPolicy(), events,
            new MutableClock(Instant.parse("2026-10-16T15:00:00Z")));
    private final AdministratorBootstrapService bootstrap = new AdministratorBootstrapService(accounts, registration);

    private static CreateUserAccountCommand admin(String email, String password, Set<RoleCode> roles) {
        return new CreateUserAccountCommand("1000000000", "Administrador", "Principal", email, "3000000000",
                password, roles);
    }

    @Test
    void anEmptyInstallationGetsItsFirstAdministrator() {
        Optional<UserAccountView> created = bootstrap.ensureAdministrator(
                admin("dueno@gmail.com", "Lavado2026!", Set.of(RoleCode.ADMIN)));

        assertThat(created).isPresent();
        assertThat(created.get().roles()).containsExactly(RoleCode.ADMIN);
        assertThat(accounts.findById(created.get().id()).orElseThrow().passwordHash().value())
                .isEqualTo("hashed:Lavado2026!");
    }

    @Test
    void theAccountIsAlwaysAnAdministratorWhateverRolesTheCommandBrings() {
        Optional<UserAccountView> created = bootstrap.ensureAdministrator(
                admin("dueno@gmail.com", "Lavado2026!", Set.of(RoleCode.CLIENT)));

        assertThat(created.orElseThrow().roles()).containsExactly(RoleCode.ADMIN);
    }

    @Test
    void ifThereIsAlreadyAnAdministratorNothingIsCreated() {
        registration.createAccount(new CreateUserAccountCommand("2000000000", "Ana", "Admin", "ana@gmail.com",
                "3001112233", "Lavado2026!", Set.of(RoleCode.ADMIN)));

        Optional<UserAccountView> created = bootstrap.ensureAdministrator(
                admin("otro@gmail.com", "Lavado2026!", Set.of(RoleCode.ADMIN)));

        assertThat(created).isEmpty();
        assertThat(accounts.count()).isEqualTo(1);
    }

    @Test
    void restartingDoesNotCreateASecondAdministrator() {
        bootstrap.ensureAdministrator(admin("dueno@gmail.com", "Lavado2026!", Set.of(RoleCode.ADMIN)));

        Optional<UserAccountView> again = bootstrap.ensureAdministrator(
                admin("dueno@gmail.com", "Lavado2026!", Set.of(RoleCode.ADMIN)));

        assertThat(again).isEmpty();
        assertThat(accounts.count()).isEqualTo(1);
    }

    @Test
    void tellsWhetherAnAdministratorExists() {
        assertThat(bootstrap.administratorExists()).isFalse();

        bootstrap.ensureAdministrator(admin("dueno@gmail.com", "Lavado2026!", Set.of(RoleCode.ADMIN)));

        assertThat(bootstrap.administratorExists()).isTrue();
    }

    @Test
    void aWeakPasswordIsRejectedLikeAnyOtherAccount() {
        assertThatThrownBy(() -> bootstrap.ensureAdministrator(admin("dueno@gmail.com", "123", Set.of(RoleCode.ADMIN))))
                .isInstanceOf(WeakPasswordException.class);
        assertThat(accounts.count()).isZero();
    }
}
