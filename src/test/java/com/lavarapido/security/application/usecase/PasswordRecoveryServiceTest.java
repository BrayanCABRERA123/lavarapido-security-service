package com.lavarapido.security.application.usecase;

import com.lavarapido.security.application.fake.Fakes.FixedResetCodeGenerator;
import com.lavarapido.security.application.fake.Fakes.InMemoryResetAttemptCounter;
import com.lavarapido.security.application.fake.Fakes.MutableClock;
import com.lavarapido.security.application.fake.Fakes.PlainPasswordHasher;
import com.lavarapido.security.application.fake.Fakes.RecordingEventPublisher;
import com.lavarapido.security.application.fake.Fakes.RecordingResetCodeNotifier;
import com.lavarapido.security.application.fake.InMemoryPasswordResetTokenRepository;
import com.lavarapido.security.application.fake.InMemoryUserAccountRepository;
import com.lavarapido.security.domain.event.PasswordChanged;
import com.lavarapido.security.domain.exception.InvalidResetCodeException;
import com.lavarapido.security.domain.exception.WeakPasswordException;
import com.lavarapido.security.domain.model.PasswordResetToken;
import com.lavarapido.security.domain.model.UserAccount;
import com.lavarapido.security.domain.port.in.RegisterUserUseCase.RegisterUserCommand;
import com.lavarapido.security.domain.port.in.RequestPasswordResetUseCase.RequestPasswordResetCommand;
import com.lavarapido.security.domain.port.in.ResetPasswordUseCase.ResetPasswordCommand;
import com.lavarapido.security.domain.port.in.VerifyPasswordResetCodeUseCase.VerifyResetCodeCommand;
import com.lavarapido.security.domain.service.PasswordPolicy;
import com.lavarapido.security.domain.service.PasswordRecoveryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordRecoveryServiceTest {

    private static final String EMAIL = "ana@gmail.com";

    private final InMemoryUserAccountRepository accounts = new InMemoryUserAccountRepository();
    private final InMemoryPasswordResetTokenRepository tokens = new InMemoryPasswordResetTokenRepository();
    private final FixedResetCodeGenerator codes = new FixedResetCodeGenerator();
    private final RecordingResetCodeNotifier notifier = new RecordingResetCodeNotifier();
    private final RecordingEventPublisher events = new RecordingEventPublisher();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-09-29T15:00:00Z"));
    private final PlainPasswordHasher hasher = new PlainPasswordHasher();

    private final PasswordRecoveryService service = new PasswordRecoveryService(accounts, tokens, hasher,
            new PasswordPolicy(), new PasswordRecoveryPolicy(Duration.ofMinutes(15), 3), codes, notifier,
            new InMemoryResetAttemptCounter(), events, clock);

    private long anaId;

    @BeforeEach
    void registerAna() {
        anaId = new AccountRegistrationService(accounts, hasher, new PasswordPolicy(), new RecordingEventPublisher(),
                clock)
                .register(new RegisterUserCommand("1023456789", "Ana", "Pérez", EMAIL, null, "Lavado2026!"))
                .id();
    }

    @Test
    void fullFlowSendsCodeVerifiesItAndSetsTheNewPassword() {
        service.requestReset(new RequestPasswordResetCommand(EMAIL, "10.0.0.1"));
        String code = notifier.lastCodeByEmail.get(EMAIL);

        assertThatCode(() -> service.verifyCode(new VerifyResetCodeCommand(EMAIL, code))).doesNotThrowAnyException();
        service.resetPassword(new ResetPasswordCommand(EMAIL, code, "Nueva2026!"));

        UserAccount ana = accounts.findById(anaId).orElseThrow();
        assertThat(hasher.matches("Nueva2026!", ana.passwordHash())).isTrue();
        assertThat(events.events).singleElement().isInstanceOf(PasswordChanged.class);
        assertThat(tokens.all()).singleElement().extracting(PasswordResetToken::usedAt).isNotNull();
    }

    @Test
    void theCodeIsStoredOnlyAsAHash() {
        service.requestReset(new RequestPasswordResetCommand(EMAIL, null));

        assertThat(tokens.all()).singleElement()
                .satisfies(token -> assertThat(token.codeHash().value()).isNotEqualTo("123456"));
    }

    @Test
    void anUnknownEmailLooksExactlyLikeAKnownOne() {
        assertThatCode(() -> service.requestReset(new RequestPasswordResetCommand("nadie@gmail.com", null)))
                .doesNotThrowAnyException();
        assertThatCode(() -> service.requestReset(new RequestPasswordResetCommand("not-an-email", null)))
                .doesNotThrowAnyException();
        assertThat(notifier.sent).isZero();
    }

    @Test
    void aCodeCannotBeUsedTwice() {
        service.requestReset(new RequestPasswordResetCommand(EMAIL, null));
        service.resetPassword(new ResetPasswordCommand(EMAIL, "123456", "Nueva2026!"));

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordCommand(EMAIL, "123456", "Otra2026!!")))
                .isInstanceOf(InvalidResetCodeException.class);
    }

    @Test
    void anExpiredCodeIsRejected() {
        service.requestReset(new RequestPasswordResetCommand(EMAIL, null));
        clock.advance(Duration.ofMinutes(16));

        assertThatThrownBy(() -> service.verifyCode(new VerifyResetCodeCommand(EMAIL, "123456")))
                .isInstanceOf(InvalidResetCodeException.class);
    }

    @Test
    void requestingAgainInvalidatesThePreviousCode() {
        service.requestReset(new RequestPasswordResetCommand(EMAIL, null));
        codes.nextCode = "654321";
        clock.advance(Duration.ofSeconds(30));
        service.requestReset(new RequestPasswordResetCommand(EMAIL, null));

        assertThatThrownBy(() -> service.verifyCode(new VerifyResetCodeCommand(EMAIL, "123456")))
                .isInstanceOf(InvalidResetCodeException.class);
        assertThatCode(() -> service.verifyCode(new VerifyResetCodeCommand(EMAIL, "654321")))
                .doesNotThrowAnyException();
    }

    @Test
    void tooManyWrongGuessesBurnTheCode() {
        service.requestReset(new RequestPasswordResetCommand(EMAIL, null));

        for (int attempt = 0; attempt < 3; attempt++) {
            assertThatThrownBy(() -> service.verifyCode(new VerifyResetCodeCommand(EMAIL, "000000")))
                    .isInstanceOf(InvalidResetCodeException.class);
        }

        // Ni el código correcto sirve ya: la fuerza bruta necesitaría un código nuevo cada 3 intentos.
        assertThatThrownBy(() -> service.verifyCode(new VerifyResetCodeCommand(EMAIL, "123456")))
                .isInstanceOf(InvalidResetCodeException.class);
    }

    @Test
    void aWeakNewPasswordDoesNotConsumeTheCode() {
        service.requestReset(new RequestPasswordResetCommand(EMAIL, null));

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordCommand(EMAIL, "123456", "weak")))
                .isInstanceOf(WeakPasswordException.class);
        assertThatCode(() -> service.resetPassword(new ResetPasswordCommand(EMAIL, "123456", "Nueva2026!")))
                .doesNotThrowAnyException();
    }

    @Test
    void aDisabledAccountCannotRecoverItsPassword() {
        UserAccount ana = accounts.findById(anaId).orElseThrow();
        ana.deactivate();
        accounts.save(ana);

        service.requestReset(new RequestPasswordResetCommand(EMAIL, null));

        assertThat(notifier.sent).isZero();
    }
}
