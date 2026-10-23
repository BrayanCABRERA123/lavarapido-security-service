package com.lavarapido.security.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Correo nuevo para iniciar sesión + la contraseña actual para confirmar. */
public record ChangeEmailRequest(
        @NotBlank @Email @Size(max = 60) String newEmail,
        @NotBlank @Size(max = 72) String currentPassword) {

    @Override
    public String toString() {
        return "ChangeEmailRequest[newEmail=" + newEmail + ", currentPassword=****]";
    }
}
