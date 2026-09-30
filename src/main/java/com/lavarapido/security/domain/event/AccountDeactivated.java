package com.lavarapido.security.domain.event;

import java.time.Instant;

/**
 * Una cuenta quedó desactivada: ya no puede iniciar sesión. Sus reservas, pagos y calificaciones
 * se conservan. Los demás servicios pueden reaccionar (ej. cancelar reservas futuras).
 *
 * @param userId        cuenta desactivada
 * @param deactivatedBy quién lo hizo: el mismo usuario o un administrador
 */
public record AccountDeactivated(long userId, long deactivatedBy, Reason reason, Instant occurredAt)
        implements DomainEvent {

    public enum Reason {
        CLOSED_BY_USER,
        DISABLED_BY_ADMIN
    }

    @Override
    public String eventType() {
        return "security.user.deactivated";
    }
}
