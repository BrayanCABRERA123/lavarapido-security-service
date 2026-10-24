package com.lavarapido.security.domain.service;

import com.lavarapido.security.domain.exception.WeakPasswordException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * La única regla de contraseñas del sistema: los mismos cuatro requisitos que muestra el frontend
 * en su lista (mínimo 8 caracteres, una mayúscula, un número y un
 * carácter especial). El tope de 72 es el límite de entrada de BCrypt, que cuenta bytes y no letras:
 * en UTF-8 una tilde o una ñ ocupa 2 bytes (SEC-11). Pasarse de 72 bytes hacía fallar el hash con un 500.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 72;

    public void validate(String rawPassword) {
        String password = rawPassword == null ? "" : rawPassword;
        List<String> violations = new ArrayList<>();

        if (password.length() < MIN_LENGTH) {
            violations.add("MIN_LENGTH");
        }
        if (password.length() > MAX_LENGTH || password.getBytes(StandardCharsets.UTF_8).length > MAX_LENGTH) {
            violations.add("MAX_LENGTH");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            violations.add("UPPERCASE");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            violations.add("DIGIT");
        }
        if (password.chars().allMatch(Character::isLetterOrDigit)) {
            violations.add("SPECIAL_CHARACTER");
        }
        if (password.chars().anyMatch(Character::isWhitespace)) {
            violations.add("NO_WHITESPACE");
        }

        if (!violations.isEmpty()) {
            throw new WeakPasswordException(violations);
        }
    }
}
