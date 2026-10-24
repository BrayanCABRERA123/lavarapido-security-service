package com.lavarapido.security.application.usecase;

import com.lavarapido.security.domain.exception.IncorrectCurrentPasswordException;
import com.lavarapido.security.domain.exception.TooManyAttemptsException;
import com.lavarapido.security.domain.model.LoginLockout;
import com.lavarapido.security.domain.model.UserAccount;
import com.lavarapido.security.domain.port.out.LoginLockoutRepository;
import com.lavarapido.security.domain.port.out.PasswordHasher;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * La contraseña actual que pide el perfil para cambiar la contraseña o el correo y para cerrar la cuenta.
 *
 * <p>SEC-12: cada error cuenta en el mismo bloqueo del login (RF-004b, security.login_lockout): 5
 * contraseñas erradas en 15 minutos, por cualquier camino, bloquean la cuenta 15 minutos. Así quien
 * encuentre una sesión abierta no puede adivinar la contraseña sin límite. Mientras dure el bloqueo
 * estas operaciones responden TOO_MANY_ATTEMPTS aunque la contraseña sea la correcta, y el login ACCOUNT_LOCKED.
 *
 * <p>Quien la use debe declarar {@code noRollbackFor} con las dos excepciones: si no, la misma
 * excepción desharía el fallo que se acaba de contar.
 */
@Component
public class CurrentPasswordVerifier {

    private final PasswordHasher passwordHasher;
    private final LoginLockoutRepository lockouts;
    private final Clock clock;

    public CurrentPasswordVerifier(PasswordHasher passwordHasher, LoginLockoutRepository lockouts, Clock clock) {
        this.passwordHasher = passwordHasher;
        this.lockouts = lockouts;
        this.clock = clock;
    }

    /**
     * @throws IncorrectCurrentPasswordException si no coincide (y queda contado)
     * @throws TooManyAttemptsException si la cuenta está bloqueada o este error la bloquea
     */
    public void verify(UserAccount account, String rawPassword) {
        // se compara antes de tomar el candado (lo lento), igual que el login
        boolean matches = passwordHasher.matches(rawPassword, account.passwordHash());
        Instant now = clock.instant();
        lockouts.lock(account.id());
        LoginLockout lockout = lockouts.findByUserId(account.id()).orElse(LoginLockout.none(account.id()));
        if (lockout.isLocked(now)) {
            throw new TooManyAttemptsException();
        }
        if (!matches) {
            LoginLockout next = lockout.registerFailure(now);
            lockouts.save(next);
            if (next.isLocked(now)) {
                throw new TooManyAttemptsException();
            }
            throw new IncorrectCurrentPasswordException();
        }
        // demostró que la conoce: los errores anteriores ya no cuentan, como al entrar bien
        if (lockout.failedCount() > 0) {
            lockouts.clear(account.id());
        }
    }
}
