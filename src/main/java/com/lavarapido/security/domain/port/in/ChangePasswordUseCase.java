package com.lavarapido.security.domain.port.in;

/** Cambiar la contraseña desde el perfil, demostrando que se conoce la actual. */
public interface ChangePasswordUseCase {

    record ChangePasswordCommand(long userId, String currentPassword, String newPassword) {

        @Override
        public String toString() {
            return "ChangePasswordCommand[userId=" + userId + ", passwords=****]";
        }
    }

    void changePassword(ChangePasswordCommand command);
}
