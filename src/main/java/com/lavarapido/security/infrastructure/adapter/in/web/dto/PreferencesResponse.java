package com.lavarapido.security.infrastructure.adapter.in.web.dto;

import com.lavarapido.security.domain.port.in.PreferenceView;

/** notificationsEnabled son las notificaciones push; los otros dos, el correo de recordatorios y las promociones. */
public record PreferencesResponse(String theme, String language, boolean notificationsEnabled,
                                  boolean emailRemindersEnabled, boolean promotionsEnabled) {

    public static PreferencesResponse from(PreferenceView view) {
        return new PreferencesResponse(view.theme(), view.language(), view.notificationsEnabled(),
                view.emailRemindersEnabled(), view.promotionsEnabled());
    }
}
