package com.lavarapido.security.domain.event;

import java.time.Instant;

/**
 * Cambió el correo con el que se inicia sesión. Solo lleva el id: quien necesite el correo nuevo
 * lo pide al security-service ({@code notification-service} puede avisar al correo anterior).
 */
public record EmailChanged(long userId, Instant occurredAt) implements DomainEvent {

    @Override
    public String eventType() {
        return "security.user.email-changed";
    }
}
