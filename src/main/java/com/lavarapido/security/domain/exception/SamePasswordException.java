package com.lavarapido.security.domain.exception;

/** La nueva contraseña es igual a la actual. */
public class SamePasswordException extends DomainException {

    public SamePasswordException() {
        super("SAME_PASSWORD", "The new password must be different from the current one");
    }
}
