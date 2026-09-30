package com.lavarapido.security.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Confirmación para cerrar la cuenta propia: la contraseña actual. */
public record DeactivateAccountRequest(@NotBlank @Size(max = 72) String currentPassword) {

    @Override
    public String toString() {
        return "DeactivateAccountRequest[****]";
    }
}
