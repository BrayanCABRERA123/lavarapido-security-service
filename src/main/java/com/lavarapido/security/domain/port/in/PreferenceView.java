package com.lavarapido.security.domain.port.in;

import com.lavarapido.security.domain.model.UserPreference;

/** Modelo de lectura de las preferencias de interfaz de una cuenta. */
public record PreferenceView(String theme, String language, boolean notificationsEnabled) {

    public static PreferenceView from(UserPreference preference) {
        return new PreferenceView(preference.theme().code(), preference.language().code(),
                preference.notificationsEnabled());
    }
}
