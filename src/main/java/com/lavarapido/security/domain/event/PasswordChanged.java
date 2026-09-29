package com.lavarapido.security.domain.event;

import java.time.Instant;

/**
 * Cambió la contraseña de una cuenta, desde el perfil o por recuperación.
 * {@code notification-service} puede avisarle al dueño por si no fue él.
 */
public record PasswordChanged(long userId, Reason reason, Instant occurredAt) implements DomainEvent {

    public enum Reason {
        CHANGED_BY_USER,
        RECOVERED
    }

    @Override
    public String eventType() {
        return "security.user.password-changed";
    }
}
