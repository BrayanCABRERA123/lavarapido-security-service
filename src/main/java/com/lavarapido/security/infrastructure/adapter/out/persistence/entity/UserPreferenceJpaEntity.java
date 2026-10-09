package com.lavarapido.security.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * {@code updated_at} y {@code row_version} son de solo lectura aquí a propósito: los maneja el
 * trigger {@code tr_touch_user_preference} (06-data/modeling-conventions.md).
 */
@Entity
@Table(schema = "security", name = "user_preference")
public class UserPreferenceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_preference_id")
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "theme", nullable = false, length = 20)
    private String theme;

    @Column(name = "`language`", nullable = false, length = 5)
    private String language;

    @Column(name = "notifications_enabled", nullable = false)
    private boolean notificationsEnabled;

    @Column(name = "email_reminders_enabled", nullable = false)
    private boolean emailRemindersEnabled = true;

    @Column(name = "promotions_enabled", nullable = false)
    private boolean promotionsEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "row_version", insertable = false, updatable = false)
    private Integer rowVersion;

    protected UserPreferenceJpaEntity() {
    }

    public UserPreferenceJpaEntity(Long userId, Instant createdAt) {
        this.userId = userId;
        this.createdAt = createdAt;
        this.createdBy = userId;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public boolean isEmailRemindersEnabled() {
        return emailRemindersEnabled;
    }

    public void setEmailRemindersEnabled(boolean emailRemindersEnabled) {
        this.emailRemindersEnabled = emailRemindersEnabled;
    }

    public boolean isPromotionsEnabled() {
        return promotionsEnabled;
    }

    public void setPromotionsEnabled(boolean promotionsEnabled) {
        this.promotionsEnabled = promotionsEnabled;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
