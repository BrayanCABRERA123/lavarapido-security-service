package com.lavarapido.security.domain.port.out;

import com.lavarapido.security.domain.model.UserAccount;

import java.time.Duration;
import java.time.Instant;

/** Emite el token de acceso firmado (JWT, ADR-006). */
public interface TokenIssuer {

    record IssuedToken(String value, Instant expiresAt) {

        @Override
        public String toString() {
            return "IssuedToken[expiresAt=" + expiresAt + "]";
        }
    }

    Duration timeToLive();

    IssuedToken issue(UserAccount account, long sessionId, Instant issuedAt);
}
