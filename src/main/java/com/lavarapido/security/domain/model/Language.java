package com.lavarapido.security.domain.model;

import com.lavarapido.security.domain.exception.InvalidValueException;

import java.util.Locale;

/** Idioma de la interfaz que acepta {@code ck_pref_language} (RF-020 pide cuatro idiomas). */
public enum Language {
    ES,
    EN,
    FR,
    PT;

    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Language fromCode(String code) {
        for (Language language : values()) {
            if (language.code().equalsIgnoreCase(code)) {
                return language;
            }
        }
        throw new InvalidValueException("INVALID_LANGUAGE", "Language must be es, en, fr or pt");
    }
}
