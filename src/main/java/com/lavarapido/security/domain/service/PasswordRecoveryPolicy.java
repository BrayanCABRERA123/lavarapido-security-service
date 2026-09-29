package com.lavarapido.security.domain.service;

import java.time.Duration;
import java.util.Objects;

/**
 * Cuánto dura un código de recuperación y cuántos intentos fallidos lo queman.
 *
 * @param codeTimeToLive    vigencia de un código desde que se pide
 * @param maxFailedAttempts intentos fallidos permitidos antes de invalidar el código
 */
public record PasswordRecoveryPolicy(Duration codeTimeToLive, int maxFailedAttempts) {

    public PasswordRecoveryPolicy {
        Objects.requireNonNull(codeTimeToLive, "codeTimeToLive");
        if (codeTimeToLive.isNegative() || codeTimeToLive.isZero()) {
            throw new IllegalArgumentException("codeTimeToLive must be positive");
        }
        if (maxFailedAttempts < 1) {
            throw new IllegalArgumentException("maxFailedAttempts must be at least 1");
        }
    }
}
