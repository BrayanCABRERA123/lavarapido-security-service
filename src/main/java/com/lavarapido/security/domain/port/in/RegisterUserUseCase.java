package com.lavarapido.security.domain.port.in;

/** Autorregistro de un cliente (RF-001). */
public interface RegisterUserUseCase {

    record RegisterUserCommand(
            String documentNumber,
            String firstName,
            String lastName,
            String email,
            String phone,
            String password) {
    }

    UserAccountView register(RegisterUserCommand command);
}
