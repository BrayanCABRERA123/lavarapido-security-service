package com.lavarapido.security.domain.port.in;

/** Paso 1 de la recuperación: enviar un código de un solo uso al correo de la cuenta (ERF1.3 / CU03). */
public interface RequestPasswordResetUseCase {

    record RequestPasswordResetCommand(String email, String ipAddress) {
    }

    /** Siempre termina en silencio, exista o no el correo, para no permitir enumerar cuentas. */
    void requestReset(RequestPasswordResetCommand command);
}
