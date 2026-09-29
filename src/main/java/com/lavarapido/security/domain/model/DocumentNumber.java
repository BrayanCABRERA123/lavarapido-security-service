package com.lavarapido.security.domain.model;

import com.lavarapido.security.domain.exception.InvalidValueException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Documento de identidad (cédula, cédula de extranjería o pasaporte). Es la única garantía fuerte de
 * que la misma persona no se registre dos veces ({@code uq_person_document}).
 */
public record DocumentNumber(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z0-9]{5,20}$");

    public DocumentNumber {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException("INVALID_DOCUMENT", "Document number is required");
        }
        value = value.strip().replace(".", "").replace(" ", "").toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new InvalidValueException("INVALID_DOCUMENT",
                    "Document number must have 5 to 20 letters or digits");
        }
    }

    public static DocumentNumber of(String value) {
        return new DocumentNumber(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
