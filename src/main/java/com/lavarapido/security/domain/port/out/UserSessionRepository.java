package com.lavarapido.security.domain.port.out;

import java.time.Instant;

/** Rastro de los inicios de sesión (IP, navegador, cuándo) en {@code security.user_session}. */
public interface UserSessionRepository {

    /** @return el id de la nueva sesión */
    long open(long userId, Instant startedAt, Instant expiresAt, String ipAddress, String userAgent);

    /** Marca la sesión como cerrada. No hace nada si no es del usuario o ya estaba cerrada. */
    void revoke(long sessionId, long userId, Instant revokedAt);

    /** Cierra todas las sesiones abiertas del usuario (cuenta desactivada: sale de todos los equipos). */
    void revokeAll(long userId, Instant revokedAt);

    /**
     * Cierra las demás sesiones abiertas del usuario y deja abierta {@code keptSessionId}
     * (cambio de contraseña: sigue adentro solo el equipo desde el que se cambió).
     */
    void revokeAllExcept(long userId, long keptSessionId, Instant revokedAt);

    /** La sesión existe, es del usuario, no se cerró y no ha vencido en {@code now}. */
    boolean isActive(long sessionId, long userId, Instant now);
}
