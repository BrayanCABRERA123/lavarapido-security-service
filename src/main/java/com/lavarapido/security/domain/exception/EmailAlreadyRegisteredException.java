package com.lavarapido.security.domain.exception;

/** El correo ya se usa como usuario de otra cuenta. */
public class EmailAlreadyRegisteredException extends DomainException {

    public EmailAlreadyRegisteredException() {
        super("EMAIL_ALREADY_REGISTERED", "An account with this email already exists");
    }
}
