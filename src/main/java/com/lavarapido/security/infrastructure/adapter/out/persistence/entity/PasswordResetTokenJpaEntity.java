package com.lavarapido.security.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(schema = "security", name = "password_reset_token")
public class PasswordResetTokenJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "password_reset_token_id")
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, length = 255, updatable = false)
    private String tokenHash;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "ip_address", length = 45, updatable = false)
    private String ipAddress;

    protected PasswordResetTokenJpaEntity() {
    }

    public PasswordResetTokenJpaEntity(Long userId, String tokenHash, Instant requestedAt, Instant expiresAt,
                                       String ipAddress) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.requestedAt = requestedAt;
        this.expiresAt = expiresAt;
        this.ipAddress = ipAddress;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }
}
