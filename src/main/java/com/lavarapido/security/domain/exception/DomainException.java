package com.lavarapido.security.domain.exception;

/**
 * Tipo base de toda violación de una regla de negocio que lanza el dominio.
 * El {@link #code()} es un identificador estable, legible por máquina, que el frontend traduce.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
