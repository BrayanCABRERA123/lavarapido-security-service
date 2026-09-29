package com.lavarapido.security.domain.port.in;

/** Paso 3 de la recuperación: consumir el código y poner la nueva contraseña. */
public interface ResetPasswordUseCase {

    record ResetPasswordCommand(String email, String code, String newPassword) {

        @Override
        public String toString() {
            return "ResetPasswordCommand[email=" + email + ", code=****, newPassword=****]";
        }
    }

    void resetPassword(ResetPasswordCommand command);
}
