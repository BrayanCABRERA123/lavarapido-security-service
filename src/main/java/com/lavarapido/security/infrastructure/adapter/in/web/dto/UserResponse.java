package com.lavarapido.security.infrastructure.adapter.in.web.dto;

import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.port.in.UserAccountView;

import java.time.Instant;
import java.util.List;

public record UserResponse(
        long id,
        String email,
        String documentNumber,
        String firstName,
        String lastName,
        String phone,
        List<String> roles,
        boolean active,
        Instant lastLogin) {

    public static UserResponse from(UserAccountView view) {
        return new UserResponse(view.id(), view.email(), view.documentNumber(), view.firstName(), view.lastName(),
                view.phone(), view.roles().stream().map(RoleCode::name).sorted().toList(), view.active(),
                view.lastLogin());
    }
}
