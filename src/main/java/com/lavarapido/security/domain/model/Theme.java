package com.lavarapido.security.domain.model;

import com.lavarapido.security.domain.exception.InvalidValueException;

/**
 * Paleta de la interfaz que acepta {@code ck_pref_theme}: las mismas cuatro que tiene la web.
 * El código es el que guarda la base y el que la web pone como clase del body.
 */
public enum Theme {
    GREEN_LIGHT("green-light"),
    GREEN_DARK("green-dark"),
    PINK("pink"),
    PINK_DARK("pink-dark");

    private final String code;

    Theme(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Theme fromCode(String code) {
        for (Theme theme : values()) {
            if (theme.code.equalsIgnoreCase(code)) {
                return theme;
            }
        }
        throw new InvalidValueException("INVALID_THEME", "Theme must be green-light, green-dark, pink or pink-dark");
    }
}
