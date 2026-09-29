package com.lavarapido.security.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Configuración del JWT (ADR-006). El secreto sale de la variable de entorno {@code JWT_SECRET} y
 * nunca se sube al repositorio; todo servicio que verifique tokens necesita el mismo valor.
 *
 * @param secret         llave HMAC, mínimo 32 bytes (256 bits) como exige HS256
 * @param issuer         claim {@code iss} que se escribe y se exige
 * @param audience       claim {@code aud} que se escribe y se exige
 * @param accessTokenTtl vigencia de un token de acceso
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, String issuer, String audience, Duration accessTokenTtl) {

    private static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "security.jwt.secret (env JWT_SECRET) must be set and be at least 32 bytes long for HS256");
        }
        if (issuer == null || issuer.isBlank() || audience == null || audience.isBlank()) {
            throw new IllegalStateException("security.jwt.issuer and security.jwt.audience must be set");
        }
        if (accessTokenTtl == null || accessTokenTtl.isNegative() || accessTokenTtl.isZero()) {
            throw new IllegalStateException("security.jwt.access-token-ttl must be a positive duration");
        }
    }
}
