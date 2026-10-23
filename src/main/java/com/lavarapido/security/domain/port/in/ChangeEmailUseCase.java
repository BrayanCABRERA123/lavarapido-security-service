package com.lavarapido.security.domain.port.in;

/**
 * El usuario cambia el correo con el que inicia sesión, confirmando con su contraseña actual.
 * Desde ese momento el login se hace con el correo nuevo.
 */
public interface ChangeEmailUseCase {

    record ChangeEmailCommand(long userId, String newEmail, String currentPassword) {

        @Override
        public String toString() {
            return "ChangeEmailCommand[userId=" + userId + ", currentPassword=****]";
        }
    }

    UserAccountView changeEmail(ChangeEmailCommand command);
}
