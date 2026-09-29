package com.lavarapido.security.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PreferencesRequest(
        @NotBlank String theme,
        @NotBlank String language,
        @NotNull Boolean notificationsEnabled) {
}
