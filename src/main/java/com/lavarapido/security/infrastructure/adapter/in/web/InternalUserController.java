package com.lavarapido.security.infrastructure.adapter.in.web;

import com.lavarapido.security.domain.port.in.GetUserProfileUseCase;
import com.lavarapido.security.domain.port.in.UserAccountView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consultas entre servicios (no las usa la web). Protegidas con la llave interna, no con el JWT
 * de un usuario (SecurityConfig.internalFilterChain).
 *
 * notification-service pide aquí el correo para enviar recordatorios y avisos de reserva: así el
 * correo vive solo en este servicio y siempre se usa el actual (ADR-011, sección 8).
 */
@RestController
@RequestMapping("/internal/v1/users")
class InternalUserController {

    private final GetUserProfileUseCase getProfile;

    InternalUserController(GetUserProfileUseCase getProfile) {
        this.getProfile = getProfile;
    }

    /** Solo lo necesario para escribirle al usuario; nada de documento, teléfono ni roles. */
    @GetMapping("/{userId}/contact")
    ContactResponse contact(@PathVariable long userId) {
        UserAccountView user = getProfile.getProfile(userId);
        return new ContactResponse(user.id(), user.email(), user.firstName(), user.active());
    }

    record ContactResponse(long userId, String email, String firstName, boolean active) {
    }
}
