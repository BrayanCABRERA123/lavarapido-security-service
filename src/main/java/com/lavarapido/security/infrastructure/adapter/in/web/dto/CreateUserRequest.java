package com.lavarapido.security.infrastructure.adapter.in.web.dto;

import com.lavarapido.security.domain.model.RoleCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

/** Un administrador crea una cuenta (ej. un operario) con una contraseña inicial. */
public record CreateUserRequest(
        @NotBlank @Size(max = 20) String documentNumber,
        @NotBlank @Size(max = 60) String firstName,
        @NotBlank @Size(max = 60) String lastName,
        @NotBlank @Email @Size(max = 60) String email,
        @Size(max = 20) String phone,
        @NotBlank @Size(max = 72) String password,
        @NotEmpty Set<RoleCode> roles) {

    @Override
    public String toString() {
        return "CreateUserRequest[email=" + email + ", roles=" + roles + ", password=****]";
    }
}
