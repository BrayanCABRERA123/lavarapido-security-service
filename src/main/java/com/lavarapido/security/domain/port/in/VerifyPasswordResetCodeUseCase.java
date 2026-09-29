package com.lavarapido.security.domain.port.in;

/** Paso 2 de la recuperación: validar el código sin consumirlo. */
public interface VerifyPasswordResetCodeUseCase {

    record VerifyResetCodeCommand(String email, String code) {

        @Override
        public String toString() {
            return "VerifyResetCodeCommand[email=" + email + ", code=****]";
        }
    }

    void verifyCode(VerifyResetCodeCommand command);
}
