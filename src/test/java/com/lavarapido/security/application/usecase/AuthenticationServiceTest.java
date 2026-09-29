package com.lavarapido.security.application.usecase;

import com.lavarapido.security.application.fake.Fakes.FakeTokenIssuer;
import com.lavarapido.security.application.fake.Fakes.InMemoryUserSessionRepository;
import com.lavarapido.security.application.fake.Fakes.MutableClock;
import com.lavarapido.security.application.fake.Fakes.PlainPasswordHasher;
import com.lavarapido.security.application.fake.Fakes.RecordingEventPublisher;
import com.lavarapido.security.application.fake.InMemoryUserAccountRepository;
import com.lavarapido.security.domain.exception.AccountDisabledException;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-29T15:00:00Z");

    private final InMemoryUserAccountRepository accounts = new InMemoryUserAccountRepository();
    private final InMemoryUserSessionRepository sessions = new InMemoryUserSessionRepository();
    private final MutableClock clock = new MutableClock(NOW);
    private final AuthenticationService service = new AuthenticationService(accounts, sessions,
            new PlainPasswordHasher(), new FakeTokenIssuer(), clock);

    private long anaId;

    @BeforeEach
    void registerAna() {
        anaId = new AccountRegistrationService(accounts, new PlainPasswordHasher(), new PasswordPolicy(),
                new RecordingEventPublisher(), clock)
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
}
