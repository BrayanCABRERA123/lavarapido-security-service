package com.lavarapido.security.domain.model;

import com.lavarapido.security.domain.exception.InvalidValueException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Correo electrónico normalizado a minúsculas. También es el usuario de login
 * ({@code app_user.username}), así que dos escrituras de la misma dirección deben ser iguales.
 */
public record EmailAddress(String value) {

    private static final int MAX_LENGTH = 120; // person.email es NVARCHAR(120)
    private static final Pattern FORMAT = Pattern.compile("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$");

    public EmailAddress {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException("INVALID_EMAIL", "Email is required");
        }
        value = value.strip().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
            throw new InvalidValueException("INVALID_EMAIL", "Email format is not valid");
        }
    }

    public static EmailAddress of(String value) {
        return new EmailAddress(value);
    }

    /** Lo que va después de la arroba. */
    public String domain() {
        return value.substring(value.indexOf('@') + 1);
    }

    /**
     * El correo es de un dominio permitido (A-01). Se exige al abrir una cuenta y al cambiar el correo;
     * no al leer cuentas ya guardadas ni al iniciar sesión, para no dejar por fuera cuentas antiguas.
     */
    public EmailAddress requireDomainIn(AllowedEmailDomains allowed) {
        if (!allowed.allows(this)) {
            throw new InvalidValueException("EMAIL_DOMAIN_NOT_ALLOWED",
                    "Only these email domains are allowed: " + String.join(", ", allowed.domains()));
        }
        return this;
    }

    @Override
    public String toString() {
        return value;
    }
}
