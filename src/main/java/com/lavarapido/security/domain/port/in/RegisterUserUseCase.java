package com.lavarapido.security.domain.port.in;

/** Autorregistro de un cliente (RF-001). */
public interface RegisterUserUseCase {

    record RegisterUserCommand(
            String documentNumber,
            String firstName,
            String lastName,
            String email,
            String phone,
            String password,
            // marcó las casillas de los Términos y de la Política de Tratamiento de Datos; las dos son
            // obligatorias (SEC-07): sin ellas el registro responde CONSENT_REQUIRED
            boolean acceptsTerms,
            boolean acceptsDataPolicy) {
    }

    UserAccountView register(RegisterUserCommand command);
}
