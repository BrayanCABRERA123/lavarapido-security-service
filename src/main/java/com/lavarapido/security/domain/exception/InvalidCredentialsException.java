package com.lavarapido.security.domain.exception;

/** Genérica a propósito: nunca revela si el correo existe o si la contraseña estaba mal. */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "Invalid email or password");
    }
}
