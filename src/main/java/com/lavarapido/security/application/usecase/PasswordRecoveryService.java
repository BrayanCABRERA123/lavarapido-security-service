package com.lavarapido.security.application.usecase;

import com.lavarapido.security.domain.event.PasswordChanged;
import com.lavarapido.security.domain.exception.InvalidResetCodeException;
import com.lavarapido.security.domain.exception.InvalidValueException;
import com.lavarapido.security.domain.model.EmailAddress;
import com.lavarapido.security.domain.model.PasswordResetToken;
import com.lavarapido.security.domain.model.UserAccount;
import com.lavarapido.security.domain.port.in.RequestPasswordResetUseCase;
import com.lavarapido.security.domain.port.in.ResetPasswordUseCase;
import com.lavarapido.security.domain.port.in.VerifyPasswordResetCodeUseCase;
import com.lavarapido.security.domain.port.out.DomainEventPublisher;
import com.lavarapido.security.domain.port.out.PasswordHasher;
import com.lavarapido.security.domain.port.out.PasswordResetTokenRepository;
import com.lavarapido.security.domain.port.out.ResetAttemptCounter;
import com.lavarapido.security.domain.port.out.ResetCodeGenerator;
import com.lavarapido.security.domain.port.out.ResetCodeNotifier;
import com.lavarapido.security.domain.port.out.UserAccountRepository;
import com.lavarapido.security.domain.service.PasswordPolicy;
import com.lavarapido.security.domain.service.PasswordRecoveryPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Flujo de contraseña olvidada en tres pasos: pedir un código, verificarlo y poner la nueva contraseña.
 *
 * <p>Los intentos fallidos se cuentan y queman el código después de {@link PasswordRecoveryPolicy#maxFailedAttempts()}
 * intentos. Esos métodos no hacen rollback con {@link InvalidResetCodeException}; si no, la
 * invalidación la desharía la misma excepción que la reporta.
 */
@Service
@Transactional
public class PasswordRecoveryService
        implements RequestPasswordResetUseCase, VerifyPasswordResetCodeUseCase, ResetPasswordUseCase {

    private static final Pattern CODE_FORMAT = Pattern.compile("^\\d{6}$");

    private final UserAccountRepository accounts;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordHasher passwordHasher;
    private final PasswordPolicy passwordPolicy;
    private final PasswordRecoveryPolicy recoveryPolicy;
    private final ResetCodeGenerator codeGenerator;
    private final ResetCodeNotifier codeNotifier;
    private final ResetAttemptCounter attemptCounter;
    private final DomainEventPublisher events;
    private final Clock clock;

    public PasswordRecoveryService(UserAccountRepository accounts, PasswordResetTokenRepository resetTokens,
                                   PasswordHasher passwordHasher, PasswordPolicy passwordPolicy,
                                   PasswordRecoveryPolicy recoveryPolicy, ResetCodeGenerator codeGenerator,
                                   ResetCodeNotifier codeNotifier, ResetAttemptCounter attemptCounter,
                                   DomainEventPublisher events, Clock clock) {
        this.accounts = accounts;
        this.resetTokens = resetTokens;
        this.passwordHasher = passwordHasher;
        this.passwordPolicy = passwordPolicy;
        this.recoveryPolicy = recoveryPolicy;
        this.codeGenerator = codeGenerator;
        this.codeNotifier = codeNotifier;
        this.attemptCounter = attemptCounter;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void requestReset(RequestPasswordResetCommand command) {
        findActiveAccount(command.email()).ifPresent(account -> {
            Instant now = clock.instant();

            // Solo vale el código más nuevo: pedir otro quema los anteriores.
            resetTokens.findRedeemableByUserId(account.id(), now).forEach(previous -> {
                previous.invalidate(now);
                resetTokens.save(previous);
            });

            String code = codeGenerator.generate();
            PasswordResetToken token = resetTokens.save(PasswordResetToken.issue(account.id(),
                    passwordHasher.hash(code), now, recoveryPolicy.codeTimeToLive(), command.ipAddress()));

            codeNotifier.send(account.username(), account.person().name(), code, token.expiresAt());
        });
    }

    @Override
    @Transactional(noRollbackFor = InvalidResetCodeException.class)
    public void verifyCode(VerifyResetCodeCommand command) {
        Instant now = clock.instant();
        UserAccount account = findActiveAccount(command.email()).orElseThrow(InvalidResetCodeException::new);
        checkCode(account, command.code(), now);
    }

    @Override
    @Transactional(noRollbackFor = InvalidResetCodeException.class)
    public void resetPassword(ResetPasswordCommand command) {
        Instant now = clock.instant();
        UserAccount account = findActiveAccount(command.email()).orElseThrow(InvalidResetCodeException::new);
        PasswordResetToken token = checkCode(account, command.code(), now);
        passwordPolicy.validate(command.newPassword());

        token.redeem(now);
        resetTokens.save(token);
        attemptCounter.clear(token.id());

        account.changePassword(passwordHasher.hash(command.newPassword()));
        accounts.save(account);
        events.publish(new PasswordChanged(account.id(), PasswordChanged.Reason.RECOVERED, now));
    }

    private PasswordResetToken checkCode(UserAccount account, String code, Instant now) {
        PasswordResetToken token = resetTokens.findLatestRedeemableByUserId(account.id(), now)
                .orElseThrow(InvalidResetCodeException::new);

        boolean matches = code != null && CODE_FORMAT.matcher(code).matches()
                && passwordHasher.matches(code, token.codeHash());
        if (!matches) {
            if (attemptCounter.recordFailure(token.id()) >= recoveryPolicy.maxFailedAttempts()) {
                token.invalidate(now);
                resetTokens.save(token);
                attemptCounter.clear(token.id());
            }
            throw new InvalidResetCodeException();
        }
        return token;
    }

    private Optional<UserAccount> findActiveAccount(String rawEmail) {
        try {
            return accounts.findByUsername(EmailAddress.of(rawEmail)).filter(UserAccount::isActive);
        } catch (InvalidValueException malformedEmail) {
            return Optional.empty();
        }
    }
}
