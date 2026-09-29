package com.lavarapido.security.domain.model;

import com.lavarapido.security.domain.exception.InvalidValueException;

/** Nombres y apellidos, cada uno limitado a los 60 caracteres que permite el esquema. */
public record PersonName(String firstName, String lastName) {

    private static final int MAX_LENGTH = 60;

    public PersonName {
        firstName = requireText(firstName, "First name");
        lastName = requireText(lastName, "Last name");
    }

    public static PersonName of(String firstName, String lastName) {
        return new PersonName(firstName, lastName);
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException("INVALID_NAME", label + " is required");
        }
        String normalized = value.strip().replaceAll("\\s+", " ");
        if (normalized.length() > MAX_LENGTH) {
            throw new InvalidValueException("INVALID_NAME", label + " is too long");
        }
        return normalized;
    }
}
