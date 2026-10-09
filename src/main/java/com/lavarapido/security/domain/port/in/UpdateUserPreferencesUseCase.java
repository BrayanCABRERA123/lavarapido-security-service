package com.lavarapido.security.domain.port.in;

/** Tema, idioma y aceptación de notificaciones (RF-020). */
public interface UpdateUserPreferencesUseCase {

    /**
     * Cambio parcial: tema, idioma y los canales (notificationsEnabled = push, emailRemindersEnabled,
     * promotionsEnabled) son opcionales; el que llegue null se queda como estaba. Así cambiar el tema no
     * toca los interruptores y guardar un interruptor no toca el tema.
     */
    record UpdatePreferencesCommand(long userId, String theme, String language, Boolean notificationsEnabled,
                                    Boolean emailRemindersEnabled, Boolean promotionsEnabled) {
    }

    PreferenceView updatePreferences(UpdatePreferencesCommand command);
}
