package com.lavarapido.security.domain.exception;

/** El código de recuperación es incorrecto, venció, ya se usó o se bloqueó por demasiados intentos. */
public class InvalidResetCodeException extends DomainException {

    public InvalidResetCodeException() {
        super("INVALID_RESET_CODE", "The recovery code is invalid or has expired");
    }
}
