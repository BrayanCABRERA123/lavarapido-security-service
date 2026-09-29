package com.lavarapido.security.domain.event;

import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.model.UserAccount;

import java.time.Instant;
import java.util.Set;

/**
 * Se creó una cuenta nueva. {@code customer-service} reacciona creando el perfil de cliente
 * ({@code customer.customer.person_id}) y {@code operations-service} reacciona al rol
 * OPERATOR. Solo lleva ids: los consumidores piden lo demás que necesiten.
 */
public record UserRegistered(long userId, long personId, Set<RoleCode> roles, Instant occurredAt)
        implements DomainEvent {

    public UserRegistered {
        roles = Set.copyOf(roles);
    }

    public static UserRegistered of(UserAccount account, Instant occurredAt) {
        return new UserRegistered(account.id(), account.person().id(), account.roles(), occurredAt);
    }

    @Override
    public String eventType() {
        return "security.user.registered";
    }
}
