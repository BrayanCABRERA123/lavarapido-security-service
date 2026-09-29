package com.lavarapido.security.domain.model;

import java.util.Objects;

/**
 * Preferencias de interfaz de una cuenta. Separadas de {@link UserAccount} a propósito:
 * cambiar el tema no debe tocar la fila de credenciales (06-data/models.md).
 */
public final class UserPreference {

    private final Long id;
    private final long userId;
    private Theme theme;
    private Language language;
    private boolean notificationsEnabled;

    private UserPreference(Long id, long userId, Theme theme, Language language, boolean notificationsEnabled) {
        this.id = id;
        this.userId = userId;
        this.theme = Objects.requireNonNull(theme, "theme");
        this.language = Objects.requireNonNull(language, "language");
        this.notificationsEnabled = notificationsEnabled;
    }

    /** Mismos valores por defecto que la tabla: tema claro, español, notificaciones activas. */
    public static UserPreference defaultsFor(long userId) {
        return new UserPreference(null, userId, Theme.LIGHT, Language.ES, true);
    }

    public static UserPreference reconstitute(Long id, long userId, Theme theme, Language language,
                                              boolean notificationsEnabled) {
        return new UserPreference(Objects.requireNonNull(id, "id"), userId, theme, language, notificationsEnabled);
    }

    public void update(Theme theme, Language language, boolean notificationsEnabled) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.language = Objects.requireNonNull(language, "language");
        this.notificationsEnabled = notificationsEnabled;
    }

    public Long id() {
        return id;
    }

    public long userId() {
        return userId;
    }

    public Theme theme() {
        return theme;
    }

    public Language language() {
        return language;
    }

    public boolean notificationsEnabled() {
        return notificationsEnabled;
    }
}
