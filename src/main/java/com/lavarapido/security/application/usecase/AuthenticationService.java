package com.lavarapido.security.application.usecase;

import com.lavarapido.security.domain.exception.InvalidCredentialsException;
import com.lavarapido.security.domain.exception.AccountLockedException;
import com.lavarapido.security.domain.exception.InvalidValueException;
import com.lavarapido.security.domain.model.EmailAddress;
import com.lavarapido.security.domain.model.HashedPassword;
import com.lavarapido.security.domain.model.UserAccount;
import com.lavarapido.security.domain.model.LoginLockout;
import com.lavarapido.security.domain.port.in.AuthenticateUserUseCase;
import com.lavarapido.security.domain.port.in.AuthenticationResult;
import com.lavarapido.security.domain.port.in.LogoutUseCase;
import com.lavarapido.security.domain.port.in.SessionStatusUseCase;
import com.lavarapido.security.domain.port.in.UserAccountView;
import com.lavarapido.security.domain.port.out.LoginLockoutRepository;
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

/** Inicio y cierre de sesión, y si la sesión de un token sigue abierta. */
@Service
@Transactional
public class AuthenticationService implements AuthenticateUserUseCase, LogoutUseCase, SessionStatusUseCase {

    private final UserAccountRepository accounts;
    private final UserSessionRepository sessions;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;
    private final LoginLockoutRepository lockouts;
    private final Clock clock;

    /**
     * Se compara contra este hash cuando el correo no existe, así un correo desconocido tarda lo mismo
     * en BCrypt que una contraseña errada y el tiempo de respuesta no revela qué cuentas existen.
     */
    private final HashedPassword timingEqualizer;

    public AuthenticationService(UserAccountRepository accounts, UserSessionRepository sessions,
                                 PasswordHasher passwordHasher, TokenIssuer tokenIssuer,
                                 LoginLockoutRepository lockouts, Clock clock) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.lockouts = lockouts;
        this.clock = clock;
        this.timingEqualizer = passwordHasher.hash("timing-equalizer-" + System.nanoTime());
    }

    /**
     * RF-004b: 5 contraseñas erradas dentro de 15 minutos bloquean la cuenta 15 minutos. Mientras está
     * bloqueada ni la contraseña correcta entra. Los fallos se guardan aunque la respuesta sea un error
     * (noRollbackFor); entrar bien borra el conteo.
     */
    @Override
    @Transactional(noRollbackFor = {InvalidCredentialsException.class, AccountLockedException.class})
    public AuthenticationResult login(LoginCommand command) {
        Instant now = clock.instant();
        Optional<UserAccount> found = findByEmail(command.email());
        boolean passwordMatches = found.map(a -> passwordHasher.matches(command.password(), a.passwordHash()))
                .orElse(false);
        if (found.isEmpty()) {
            throw new InvalidCredentialsException();
        }
        UserAccount account = found.get();
        // después de comparar la contraseña (lo lento) para no tener el candado mientras tanto
        lockouts.lock(account.id());
        LoginLockout lockout = lockouts.findByUserId(account.id()).orElse(LoginLockout.none(account.id()));
        if (lockout.isLocked(now)) {
            throw new AccountLockedException(lockout.lockedUntil());
        }
        if (!passwordMatches) {
            LoginLockout next = lockout.registerFailure(now);
            lockouts.save(next);
            if (next.isLocked(now)) {
                throw new AccountLockedException(next.lockedUntil());
            }
            throw new InvalidCredentialsException();
        }

        // Se valida solo después de la contraseña: que está desactivada solo lo sabe su dueño.
        account.ensureCanAuthenticate();

        lockouts.clear(account.id());
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

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(long userId, long sessionId) {
        return sessions.isActive(sessionId, userId, clock.instant());
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
