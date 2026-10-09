package com.lavarapido.security.domain.model;

import java.util.Objects;

/**
 * Preferencias de una cuenta: interfaz (tema, idioma) y por qué canales quiere sus avisos.
 * Separadas de {@link UserAccount} a propósito: cambiar el tema no debe tocar la fila de
 * credenciales (06-data/models.md).
 *
 * Canales: notificationsEnabled son las notificaciones push del celular; emailRemindersEnabled, el
 * correo de los recordatorios de reserva; promotionsEnabled, los avisos de promociones (cupones
 * desbloqueados). notification-service los respeta al enviar; la bandeja de la app siempre se llena.
 */
public final class UserPreference {

    private final Long id;
    private final long userId;
    private Theme theme;
    private Language language;
    private boolean notificationsEnabled;
    private boolean emailRemindersEnabled;
    private boolean promotionsEnabled;

    private UserPreference(Long id, long userId, Theme theme, Language language, boolean notificationsEnabled,
                           boolean emailRemindersEnabled, boolean promotionsEnabled) {
        this.id = id;
        this.userId = userId;
        this.theme = Objects.requireNonNull(theme, "theme");
        this.language = Objects.requireNonNull(language, "language");
        this.notificationsEnabled = notificationsEnabled;
        this.emailRemindersEnabled = emailRemindersEnabled;
        this.promotionsEnabled = promotionsEnabled;
    }

    /** Mismos valores por defecto que la tabla: paleta verde clara, español y todos los canales activos. */
    public static UserPreference defaultsFor(long userId) {
        return new UserPreference(null, userId, Theme.GREEN_LIGHT, Language.ES, true, true, true);
    }

    public static UserPreference reconstitute(Long id, long userId, Theme theme, Language language,
                                              boolean notificationsEnabled, boolean emailRemindersEnabled,
                                              boolean promotionsEnabled) {
        return new UserPreference(Objects.requireNonNull(id, "id"), userId, theme, language, notificationsEnabled,
                emailRemindersEnabled, promotionsEnabled);
    }

    /** Tema e idioma de la interfaz. */
    public void changeInterface(Theme theme, Language language) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.language = Objects.requireNonNull(language, "language");
    }

    /** Canales de notificación; el que llegue null se queda como está (cambiar el tema no los toca). */
    public void changeNotificationChannels(Boolean push, Boolean emailReminders, Boolean promotions) {
        if (push != null) {
            this.notificationsEnabled = push;
        }
        if (emailReminders != null) {
            this.emailRemindersEnabled = emailReminders;
        }
        if (promotions != null) {
            this.promotionsEnabled = promotions;
        }
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

    public boolean emailRemindersEnabled() {
        return emailRemindersEnabled;
    }

    public boolean promotionsEnabled() {
        return promotionsEnabled;
    }
}
