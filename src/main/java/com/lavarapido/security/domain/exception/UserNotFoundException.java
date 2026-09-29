package com.lavarapido.security.domain.exception;

/** La cuenta solicitada no existe. */
public class UserNotFoundException extends DomainException {

    public UserNotFoundException() {
        super("USER_NOT_FOUND", "User not found");
    }
}
