package com.lavarapido.security.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Intentos fallidos de inicio de sesión de una cuenta y su bloqueo (RF-004b): 5 contraseñas erradas
 * dentro de 15 minutos bloquean la cuenta 15 minutos. Un inicio de sesión correcto o restablecer la
 * contraseña lo borran. Es inmutable: cada cambio devuelve el estado nuevo.
 *
 * @param failedCount   fallos dentro de la ventana actual
 * @param firstFailedAt cuándo empezó la ventana (el primer fallo); null sin fallos
 * @param lockedUntil   hasta cuándo está bloqueada; null si no lo está
 */
public record LoginLockout(long userId, int failedCount, Instant firstFailedAt, Instant lockedUntil) {

    public static final int MAX_FAILURES = 5;
    public static final Duration WINDOW = Duration.ofMinutes(15);
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    public static LoginLockout none(long userId) {
        return new LoginLockout(userId, 0, null, null);
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    /**
     * Un fallo más. Si la ventana de 15 minutos ya pasó, empieza otra; al quinto fallo dentro de la
     * ventana la cuenta queda bloqueada y el conteo vuelve a cero.
     */
    public LoginLockout registerFailure(Instant now) {
        Objects.requireNonNull(now, "now");
        boolean windowOpen = firstFailedAt != null && now.isBefore(firstFailedAt.plus(WINDOW));
        int count = windowOpen ? failedCount + 1 : 1;
        Instant start = windowOpen ? firstFailedAt : now;
        if (count >= MAX_FAILURES) {
            return new LoginLockout(userId, 0, null, now.plus(LOCK_DURATION));
        }
        return new LoginLockout(userId, count, start, isLocked(now) ? lockedUntil : null);
    }
}
