package com.lavarapido.security.infrastructure.adapter.in.web.dto;

import com.lavarapido.security.domain.port.in.PreferenceView;

public record PreferencesResponse(String theme, String language, boolean notificationsEnabled) {

    public static PreferencesResponse from(PreferenceView view) {
        return new PreferencesResponse(view.theme(), view.language(), view.notificationsEnabled());
    }
}
