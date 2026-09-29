package com.lavarapido.security.application.usecase;

import com.lavarapido.security.domain.exception.InvalidCredentialsException;
import com.lavarapido.security.domain.exception.InvalidValueException;
import com.lavarapido.security.domain.model.EmailAddress;
import com.lavarapido.security.domain.model.HashedPassword;
import com.lavarapido.security.domain.model.UserAccount;
import com.lavarapido.security.domain.port.in.AuthenticateUserUseCase;
import com.lavarapido.security.domain.port.in.AuthenticationResult;
import com.lavarapido.security.domain.port.in.LogoutUseCase;
import com.lavarapido.security.domain.port.in.UserAccountView;
import com.lavarapido.security.domain.port.out.PasswordHasher;
import com.lavarapido.security.domain.port.out.TokenIssuer;
import com.lavarapido.security.domain.port.out.TokenIssuer.IssuedToken;
import com.lavarapido.security.domain.port.out.UserAccountRepository;
import com.lavarapido.security.domain.port.out.UserSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/** Inicio y cierre de sesión. */
@Service
@Transactional
public class AuthenticationService implements AuthenticateUserUseCase, LogoutUseCase {

    private final UserAccountRepository accounts;
    private final UserSessionRepository sessions;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;
    private final Clock clock;

    /**
     * Se compara contra este hash cuando el correo no existe, así un correo desconocido tarda lo mismo
     * en BCrypt que una contraseña errada y el tiempo de respuesta no revela qué cuentas existen.
     */
    private final HashedPassword timingEqualizer;

    public AuthenticationService(UserAccountRepository accounts, UserSessionRepository sessions,
                                 PasswordHasher passwordHasher, TokenIssuer tokenIssuer, Clock clock) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.clock = clock;
        this.timingEqualizer = passwordHasher.hash("timing-equalizer-" + System.nanoTime());
    }

    @Override
    public AuthenticationResult login(LoginCommand command) {
        UserAccount account = findByEmail(command.email())
                .filter(found -> passwordHasher.matches(command.password(), found.passwordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        // Se valida solo después de la contraseña: que está desactivada solo lo sabe su dueño.
        account.ensureCanAuthenticate();

        Instant now = clock.instant();
        account.recordSuccessfulLogin(now);
        accounts.save(account);

        long sessionId = sessions.open(account.id(), now, now.plus(tokenIssuer.timeToLive()),
                command.ipAddress(), command.userAgent());
        IssuedToken token = tokenIssuer.issue(account, sessionId, now);

        return new AuthenticationResult(token.value(), token.expiresAt(), UserAccountView.from(account));
    }

    @Override
    public void logout(long userId, long sessionId) {
        sessions.revoke(sessionId, userId, clock.instant());
    }

    private Optional<UserAccount> findByEmail(String rawEmail) {
        Optional<UserAccount> account;
        try {
            account = accounts.findByUsername(EmailAddress.of(rawEmail));
        } catch (InvalidValueException malformedEmail) {
            account = Optional.empty();
        }
        if (account.isEmpty()) {
            passwordHasher.matches("not-a-real-password", timingEqualizer);
        }
        return account;
    }
}
