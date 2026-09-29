package com.lavarapido.security.domain.port.out;

import java.time.Instant;

/** Rastro de los inicios de sesión (IP, navegador, cuándo) en {@code security.user_session}. */
public interface UserSessionRepository {

    /** @return el id de la nueva sesión */
    long open(long userId, Instant startedAt, Instant expiresAt, String ipAddress, String userAgent);

    /** Marca la sesión como cerrada. No hace nada si no es del usuario o ya estaba cerrada. */
    void revoke(long sessionId, long userId, Instant revokedAt);
}
