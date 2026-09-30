package com.lavarapido.security.domain.event;

import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.model.UserAccount;

import java.time.Instant;
import java.util.Set;

/**
 * Se creó una cuenta nueva. {@code customer-service} reacciona creando el perfil de cliente
 * ({@code customer.customer.person_id}), {@code operations-service} reacciona al rol OPERATOR y
 * {@code notification-service} le da la bienvenida (en la app y por correo).
 *
 * Lleva el correo y el primer nombre solo para el correo de bienvenida (ADR-011): así el
 * notification-service no tiene que consultar la base de seguridad. No lleva nada más personal.
 */
public record UserRegistered(long userId, long personId, String email, String firstName, Set<RoleCode> roles,
                             Instant occurredAt) implements DomainEvent {

    public UserRegistered {
        roles = Set.copyOf(roles);
    }

    public static UserRegistered of(UserAccount account, Instant occurredAt) {
        return new UserRegistered(account.id(), account.person().id(), account.username().value(),
                account.person().name().firstName(), account.roles(), occurredAt);
    }

    /** Sin correo ni nombre: el evento se escribe en los logs y un log no debe tener datos personales. */
    @Override
    public String toString() {
        return "UserRegistered[userId=" + userId + ", personId=" + personId + ", roles=" + roles
                + ", occurredAt=" + occurredAt + "]";
    }

    @Override
    public String eventType() {
        // routing key de cross-cutting.md §7: <esquema>.<evento_en_snake_case>
        return "security.user_registered";
    }
}
