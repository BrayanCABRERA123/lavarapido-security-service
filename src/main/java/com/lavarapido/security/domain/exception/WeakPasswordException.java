package com.lavarapido.security.domain.exception;

import java.util.List;

/** La contraseña no cumple la política de contraseñas. */
public class WeakPasswordException extends DomainException {

    private final List<String> violations;

    public WeakPasswordException(List<String> violations) {
        super("WEAK_PASSWORD", "Password does not meet the policy: " + String.join(", ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> violations() {
        return violations;
    }
}
