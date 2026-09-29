package com.lavarapido.security.domain.model;

import com.lavarapido.security.domain.exception.InvalidValueException;

import java.util.Locale;

/** Tema de la interfaz que acepta {@code ck_pref_theme}. */
public enum Theme {
    LIGHT,
    DARK;

    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Theme fromCode(String code) {
        for (Theme theme : values()) {
            if (theme.code().equalsIgnoreCase(code)) {
                return theme;
            }
        }
        throw new InvalidValueException("INVALID_THEME", "Theme must be light or dark");
    }
}
