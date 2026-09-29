package com.lavarapido.security.domain.port.in;

/** Tema, idioma y aceptación de notificaciones (RF-020). */
public interface UpdateUserPreferencesUseCase {

    record UpdatePreferencesCommand(long userId, String theme, String language, boolean notificationsEnabled) {
    }

    PreferenceView updatePreferences(UpdatePreferencesCommand command);
}
