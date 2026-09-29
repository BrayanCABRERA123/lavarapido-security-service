package com.lavarapido.security.domain.exception;

/** Un administrador intentó desactivar la cuenta con la que tiene la sesión abierta. */
public class CannotDisableOwnAccountException extends DomainException {

    public CannotDisableOwnAccountException() {
        super("CANNOT_DISABLE_OWN_ACCOUNT", "You cannot disable your own account");
    }
}
