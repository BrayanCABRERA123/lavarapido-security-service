package com.lavarapido.security.infrastructure.adapter.in.web.dto;

import java.time.Instant;

/** Respuesta de token estilo OAuth2, más el usuario para que el frontend redirija por rol sin otra llamada. */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Instant expiresAt,
        UserResponse user) {
}
