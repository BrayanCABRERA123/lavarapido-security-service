package com.lavarapido.security.domain.model;

import com.lavarapido.security.domain.exception.InvalidValueException;

import java.util.regex.Pattern;

/**
 * Teléfono de contacto: de 7 a 15 dígitos, opcionalmente con {@code +}. Más amplio a propósito que
 * el formulario de registro (celular colombiano, 10 dígitos que empiezan en 3) para que los fijos
 * que se digitan en el mostrador sigan siendo válidos.
 */
public record PhoneNumber(String value) {

    private static final Pattern FORMAT = Pattern.compile("^\\+?\\d{7,15}$");

    public PhoneNumber {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException("INVALID_PHONE", "Phone number is required");
        }
        value = value.replaceAll("[\\s()-]", "");
        if (!FORMAT.matcher(value).matches()) {
            throw new InvalidValueException("INVALID_PHONE", "Phone number must have 7 to 15 digits");
        }
    }

    /** El teléfono es opcional en {@code person}; vacío significa "sin teléfono". */
    public static PhoneNumber ofNullable(String value) {
        return value == null || value.isBlank() ? null : new PhoneNumber(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
