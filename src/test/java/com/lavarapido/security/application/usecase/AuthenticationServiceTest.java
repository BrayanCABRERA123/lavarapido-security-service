package com.lavarapido.security.application.usecase;

import com.lavarapido.security.domain.model.AllowedEmailDomains;
import java.util.Set;
import com.lavarapido.security.domain.port.in.CreateUserAccountUseCase.CreateUserAccountCommand;
import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.application.fake.Fakes.FakeTokenIssuer;
import com.lavarapido.security.application.fake.Fakes.InMemoryLoginLockoutRepository;
import com.lavarapido.security.application.fake.Fakes.InMemoryUserSessionRepository;
import com.lavarapido.security.application.fake.Fakes.MutableClock;
import com.lavarapido.security.application.fake.Fakes.PlainPasswordHasher;
import com.lavarapido.security.application.fake.Fakes.RecordingEventPublisher;
import com.lavarapido.security.application.fake.InMemoryUserAccountRepository;
import com.lavarapido.security.domain.exception.AccountDisabledException;
import com.lavarapido.security.domain.exception.AccountLockedException;
import com.lavarapido.security.domain.exception.InvalidCredentialsException;
import com.lavarapido.security.domain.model.UserAccount;
import com.lavarapido.security.domain.port.in.AuthenticateUserUseCase.LoginCommand;
import com.lavarapido.security.domain.port.in.AuthenticationResult;
import com.lavarapido.security.domain.port.in.RegisterUserUseCase.RegisterUserCommand;
import com.lavarapido.security.domain.service.PasswordPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-29T15:00:00Z");

    private final InMemoryUserAccountRepository accounts = new InMemoryUserAccountRepository();
    private final InMemoryUserSessionRepository sessions = new InMemoryUserSessionRepository();
    private final MutableClock clock = new MutableClock(NOW);
    private final InMemoryLoginLockoutRepository lockouts = new InMemoryLoginLockoutRepository();
    private final AuthenticationService service = new AuthenticationService(accounts, sessions,
            new PlainPasswordHasher(), new FakeTokenIssuer(), lockouts, clock);

    private long anaId;

    @BeforeEach
    void registerAna() {
        anaId = new AccountRegistrationService(accounts, new PlainPasswordHasher(), new PasswordPolicy(),
                new RecordingEventPublisher(), clock, AllowedEmailDomains.parse("gmail.com"))
                .register(new RegisterUserCommand("1023456789", "Ana", "Pérez", "ana@gmail.com", null, "Lavado2026!"))
                .id();
    }

    @Test
    void validCredentialsReturnATokenAndOpenASession() {
        AuthenticationResult result = service.login(new LoginCommand("ANA@gmail.com", "Lavado2026!", "10.0.0.1", "JUnit"));

        assertThat(result.accessToken()).isEqualTo("token-for-" + anaId + "-session-1");
        assertThat(result.expiresAt()).isEqualTo(NOW.plusSeconds(3600));
        assertThat(result.user().email()).isEqualTo("ana@gmail.com");
        assertThat(sessions.sessions.get(1L).ipAddress()).isEqualTo("10.0.0.1");
        assertThat(accounts.findById(anaId).orElseThrow().lastLogin()).isEqualTo(NOW);
    }

    @ParameterizedTest(name = "email={0}")
    @CsvSource({
            "ana@gmail.com,     WrongPass1!",
            "nadie@gmail.com,   Lavado2026!",
            "not-an-email,      Lavado2026!"
    })
    void wrongEmailOrPasswordGiveTheSameGenericError(String email, String password) {
        assertThatThrownBy(() -> service.login(new LoginCommand(email, password, null, null)))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(sessions.sessions).isEmpty();
    }

    @Test
    void aDisabledAccountIsToldSoOnlyAfterTheRightPassword() {
        UserAccount ana = accounts.findById(anaId).orElseThrow();
        ana.deactivate();
        accounts.save(ana);

        assertThatThrownBy(() -> service.login(new LoginCommand("ana@gmail.com", "WrongPass1!", null, null)))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> service.login(new LoginCommand("ana@gmail.com", "Lavado2026!", null, null)))
                .isInstanceOf(AccountDisabledException.class);
    }

    @Test
    void logoutClosesOnlyTheCallersOwnSession() {
        service.login(new LoginCommand("ana@gmail.com", "Lavado2026!", null, null));

        service.logout(anaId + 99, 1L);
        assertThat(sessions.sessions.get(1L).revokedAt()).isNull();

        service.logout(anaId, 1L);
        assertThat(sessions.sessions.get(1L).revokedAt()).isEqualTo(NOW);
    }

    @Test
    void aSessionStaysActiveOnlyUntilItIsClosedOrExpires() {
        service.login(new LoginCommand("ana@gmail.com", "Lavado2026!", null, null));
        assertThat(service.isActive(anaId, 1L)).isTrue();
        // la sesión de otro usuario con el mismo id no cuenta
        assertThat(service.isActive(anaId + 99, 1L)).isFalse();

        clock.advance(Duration.ofMinutes(61));
        assertThat(service.isActive(anaId, 1L)).isFalse();
    }

    @Test
    void removingARoleInTheDatabaseClosesTheSessionsOfTokensThatStillCarryIt() {
        // E-02: retirar a un administrador (se hace en security.user_role) ya no deja su token vivo
        long bossId = new AccountRegistrationService(accounts, new PlainPasswordHasher(), new PasswordPolicy(),
                new RecordingEventPublisher(), clock, AllowedEmailDomains.parse("gmail.com"))
                .createAccount(new CreateUserAccountCommand("80000009", "Jefe", "Ruiz", "jefe@gmail.com", null,
                        "Lavado2026!", Set.of(RoleCode.ADMIN)))
                .id();
        service.login(new LoginCommand("jefe@gmail.com", "Lavado2026!", null, null));
        assertThat(service.isActive(bossId, 1L, Set.of(RoleCode.ADMIN))).isTrue();

        UserAccount boss = accounts.findById(bossId).orElseThrow();
        accounts.save(UserAccount.reconstitute(bossId, boss.person(), boss.username(), boss.passwordHash(), true,
                boss.lastLogin(), Set.of(RoleCode.CLIENT)));

        assertThat(service.isActive(bossId, 1L, Set.of(RoleCode.ADMIN))).isFalse();
        // y se revocan todas sus sesiones: tampoco sirve otro token de esa persona
        assertThat(sessions.openSessionsOf(bossId)).isZero();
        assertThat(service.isActive(bossId, 1L, Set.of(RoleCode.CLIENT))).isFalse();
    }

    @Test
    void aSessionIsNoLongerActiveAfterLogout() {
        service.login(new LoginCommand("ana@gmail.com", "Lavado2026!", null, null));

        service.logout(anaId, 1L);

        assertThat(service.isActive(anaId, 1L)).isFalse();
    }

    private void failLogin() {
        assertThatThrownBy(() -> service.login(new LoginCommand("ana@gmail.com", "WrongPass1!", null, null)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void fiveWrongPasswordsWithinFifteenMinutesLockTheAccountFifteenMinutes() {
        for (int i = 0; i < 4; i++) {
            failLogin();
            clock.advance(Duration.ofMinutes(2));
        }
        // el quinto fallo ya responde que la cuenta quedó bloqueada
        assertThatThrownBy(() -> service.login(new LoginCommand("ana@gmail.com", "WrongPass1!", null, null)))
                .isInstanceOf(AccountLockedException.class);
        // bloqueada, ni la contraseña correcta entra
        clock.advance(Duration.ofMinutes(14));
        assertThatThrownBy(() -> service.login(new LoginCommand("ana@gmail.com", "Lavado2026!", null, null)))
                .isInstanceOf(AccountLockedException.class);
        assertThat(sessions.sessions).isEmpty();
        // pasados los 15 minutos vuelve a entrar y el conteo se borra
        clock.advance(Duration.ofMinutes(1));
        assertThat(service.login(new LoginCommand("ana@gmail.com", "Lavado2026!", null, null)).accessToken()).isNotBlank();
        assertThat(lockouts.rows).isEmpty();
    }

    @Test
    void failuresSpreadOverMoreThanFifteenMinutesDoNotLock() {
        for (int i = 0; i < 6; i++) {
            failLogin();
            clock.advance(Duration.ofMinutes(4));
        }
        assertThat(service.login(new LoginCommand("ana@gmail.com", "Lavado2026!", null, null)).accessToken()).isNotBlank();
    }

    @Test
    void aSuccessfulLoginResetsTheFailures() {
        for (int i = 0; i < 4; i++) {
            failLogin();
        }
        service.login(new LoginCommand("ana@gmail.com", "Lavado2026!", null, null));
        for (int i = 0; i < 4; i++) {
            failLogin();
        }
        assertThat(lockouts.rows.get(anaId).failedCount()).isEqualTo(4);
    }

    @Test
    void theAttemptsAreLockedBeforeBeingReadSoConcurrentFailuresAreNotLost() {
        failLogin();
        service.login(new LoginCommand("ana@gmail.com", "Lavado2026!", null, null));

        assertThat(lockouts.locked).containsExactly(anaId);
        assertThat(lockouts.unlockedReads).isZero();
    }

    @Test
    void unknownEmailsAreNeverLockedAndKeepTheGenericError() {
        for (int i = 0; i < 6; i++) {
            assertThatThrownBy(() -> service.login(new LoginCommand("nadie@gmail.com", "Lavado2026!", null, null)))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
        assertThat(lockouts.rows).isEmpty();
    }
}
