package com.lavarapido.security.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Autorregistro. Los value objects del dominio vuelven a validar el formato. */
public record RegisterRequest(
        @NotBlank @Size(max = 20) String documentNumber,
        @NotBlank @Size(max = 60) String firstName,
        @NotBlank @Size(max = 60) String lastName,
        @NotBlank @Email @Size(max = 60) String email,
        @Size(max = 20) String phone,
        @NotBlank @Size(max = 72) String password,
        // opcionales para no romper versiones anteriores de la app; la web y la app nuevas los envían
        Boolean acceptsTerms,
        Boolean acceptsDataPolicy) {

    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + ", password=****]";
    }
}
