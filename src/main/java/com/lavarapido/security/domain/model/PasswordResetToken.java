package com.lavarapido.security.domain.model;

import com.lavarapido.security.domain.exception.InvalidResetCodeException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Código de un solo uso para recuperar la contraseña (ERF1.3 / CU03). Solo se guarda su hash.
 *
 * <p>Invariantes: vence al cumplirse su tiempo de vida y se puede canjear máximo una vez
 * ({@code used_at} impide reutilizarlo).
 */
public final class PasswordResetToken {

    private final Long id;
    private final long userId;
    private final HashedPassword codeHash;
    private final Instant requestedAt;
    private final Instant expiresAt;
    private Instant usedAt;
    private final String ipAddress;

    private PasswordResetToken(Long id, long userId, HashedPassword codeHash, Instant requestedAt,
                               Instant expiresAt, Instant usedAt, String ipAddress) {
        this.id = id;
        this.userId = userId;
        this.codeHash = Objects.requireNonNull(codeHash, "codeHash");
        this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(requestedAt)) {
            throw new IllegalArgumentException("expiresAt must be after requestedAt");
        }
        this.usedAt = usedAt;
        this.ipAddress = ipAddress;
    }

    public static PasswordResetToken issue(long userId, HashedPassword codeHash, Instant now,
                                           Duration timeToLive, String ipAddress) {
        return new PasswordResetToken(null, userId, codeHash, now, now.plus(timeToLive), null, ipAddress);
    }

    public static PasswordResetToken reconstitute(Long id, long userId, HashedPassword codeHash, Instant requestedAt,
                                                  Instant expiresAt, Instant usedAt, String ipAddress) {
        return new PasswordResetToken(Objects.requireNonNull(id, "id"), userId, codeHash, requestedAt,
                expiresAt, usedAt, ipAddress);
    }

    public boolean isRedeemableAt(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    public void ensureRedeemableAt(Instant now) {
        if (!isRedeemableAt(now)) {
            throw new InvalidResetCodeException();
        }
    }

    /** Consume el código: después de esto no se puede volver a usar. */
    public void redeem(Instant now) {
        ensureRedeemableAt(now);
        this.usedAt = now;
    }

    /** Quema el código sin canjearlo (se emitió uno más nuevo o hubo demasiados intentos). */
    public void invalidate(Instant now) {
        if (usedAt == null) {
            this.usedAt = now;
        }
    }

    public Long id() {
        return id;
    }

    public long userId() {
        return userId;
    }

    public HashedPassword codeHash() {
        return codeHash;
    }

    public Instant requestedAt() {
        return requestedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant usedAt() {
        return usedAt;
    }

    public String ipAddress() {
        return ipAddress;
    }
}
