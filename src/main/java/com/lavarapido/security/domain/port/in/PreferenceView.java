package com.lavarapido.security.domain.port.in;

import com.lavarapido.security.domain.model.UserPreference;

/**
 * Modelo de lectura de las preferencias de una cuenta: interfaz y canales de notificación
 * (push, correo de recordatorios y promociones).
 */
public record PreferenceView(String theme, String language, boolean notificationsEnabled,
                             boolean emailRemindersEnabled, boolean promotionsEnabled) {

    public static PreferenceView from(UserPreference preference) {
        return new PreferenceView(preference.theme().code(), preference.language().code(),
                preference.notificationsEnabled(), preference.emailRemindersEnabled(),
                preference.promotionsEnabled());
    }
}
