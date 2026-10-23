package com.lavarapido.security.domain.port.in;

/**
 * Cambiar la contraseña desde el perfil, demostrando que se conoce la actual.
 * {@code sessionId} es la sesión desde la que se cambia: queda abierta y se cierran las demás.
 */
public interface ChangePasswordUseCase {

    record ChangePasswordCommand(long userId, long sessionId, String currentPassword, String newPassword) {

        @Override
        public String toString() {
            return "ChangePasswordCommand[userId=" + userId + ", sessionId=" + sessionId + ", passwords=****]";
        }
    }

    void changePassword(ChangePasswordCommand command);
}
