package com.lavarapido.security.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyResetCodeRequest(
        @NotBlank @Size(max = 120) String email,
        @NotBlank @Pattern(regexp = "[0-9]{6}") String code) {

    @Override
    public String toString() {
        return "VerifyResetCodeRequest[email=" + email + ", code=****]";
    }
}
