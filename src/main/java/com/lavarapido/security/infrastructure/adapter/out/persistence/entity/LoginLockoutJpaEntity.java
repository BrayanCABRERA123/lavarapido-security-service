package com.lavarapido.security.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Fallos de inicio de sesión y bloqueo de una cuenta (security.login_lockout, RF-004b). */
@Entity
@Table(schema = "security", name = "login_lockout")
public class LoginLockoutJpaEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "failed_count")
    private Integer failedCount;

    @Column(name = "first_failed_at")
    private Instant firstFailedAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected LoginLockoutJpaEntity() {
    }

    public LoginLockoutJpaEntity(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public Instant getFirstFailedAt() {
        return firstFailedAt;
    }

    public void setFirstFailedAt(Instant firstFailedAt) {
        this.firstFailedAt = firstFailedAt;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
