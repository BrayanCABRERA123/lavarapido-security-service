package com.lavarapido.security.domain.exception;

/**
 * Demasiados intentos fallidos de un secreto que ya se pidió una vez: el código de recuperación
 * (SEC-05) o la contraseña actual en el perfil (SEC-12). Responde 423 TOO_MANY_ATTEMPTS.
 */
public class TooManyAttemptsException extends DomainException {

    public TooManyAttemptsException() {
        super("TOO_MANY_ATTEMPTS", "Too many failed attempts: try again later");
    }
}
