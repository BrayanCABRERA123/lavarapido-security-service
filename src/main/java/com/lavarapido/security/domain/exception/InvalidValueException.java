package com.lavarapido.security.domain.exception;

/** Un value object rechazó su entrada (correo, documento, teléfono, nombre... mal formados). */
public class InvalidValueException extends DomainException {

    public InvalidValueException(String code, String message) {
        super(code, message);
    }
}
