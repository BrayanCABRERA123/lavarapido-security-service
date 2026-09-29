package com.lavarapido.security.domain.port.in;

import java.time.Instant;

/** Un login exitoso: el token firmado, cuándo vence y quién inició sesión. */
public record AuthenticationResult(String accessToken, Instant expiresAt, UserAccountView user) {
}
