package com.lavarapido.security.domain.port.in;

import com.lavarapido.security.domain.model.PhoneNumber;
import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.model.UserAccount;

import java.time.Instant;
import java.util.Set;

/** Modelo de lectura de una cuenta para los adaptadores, así el agregado nunca sale del núcleo. */
public record UserAccountView(
        long id,
        String email,
        String documentNumber,
        String firstName,
        String lastName,
        String phone,
        Set<RoleCode> roles,
        boolean active,
        Instant lastLogin) {

    public UserAccountView {
        roles = Set.copyOf(roles);
    }

    public static UserAccountView from(UserAccount account) {
        PhoneNumber phone = account.person().phone();
        return new UserAccountView(
                account.id(),
                account.username().value(),
                account.person().documentNumber().value(),
                account.person().name().firstName(),
                account.person().name().lastName(),
                phone == null ? null : phone.value(),
                account.roles(),
                account.isActive(),
                account.lastLogin());
    }
}
