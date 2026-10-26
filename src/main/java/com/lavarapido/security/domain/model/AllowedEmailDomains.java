package com.lavarapido.security.domain.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dominios de correo con los que se puede abrir una cuenta o cambiar el correo (A-01, RF-001). Se
 * configura con ALLOWED_EMAIL_DOMAINS (lista separada por comas); por defecto solo gmail.com. La
 * comparación es exacta: "gmail.com.co" o "mail.gmail.com" no son gmail.com.
 */
public record AllowedEmailDomains(Set<String> domains) {

    public static final String DEFAULT = "gmail.com";

    public AllowedEmailDomains {
        domains = Set.copyOf(domains);
    }

    /** "gmail.com, outlook.com" → {gmail.com, outlook.com}; vacía: solo gmail.com. */
    public static AllowedEmailDomains parse(String csv) {
        Set<String> parsed = csv == null ? Set.of() : Arrays.stream(csv.split(","))
                .map(domain -> domain.strip().toLowerCase(Locale.ROOT))
                .filter(domain -> !domain.isEmpty())
                .collect(Collectors.toSet());
        return new AllowedEmailDomains(parsed.isEmpty() ? Set.of(DEFAULT) : parsed);
    }

    public boolean allows(EmailAddress email) {
        return domains.contains(email.domain());
    }
}
