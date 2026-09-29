package com.lavarapido.security.domain.exception;

/** Para cambiar la contraseña hay que demostrar que se conoce la actual. */
public class IncorrectCurrentPasswordException extends DomainException {

    public IncorrectCurrentPasswordException() {
        super("INCORRECT_CURRENT_PASSWORD", "The current password is incorrect");
    }
}
