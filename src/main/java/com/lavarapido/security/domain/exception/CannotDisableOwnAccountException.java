package com.lavarapido.security.domain.exception;

/** Un administrador intentó desactivar su propia cuenta (desde Gestión o desde su perfil). */
public class CannotDisableOwnAccountException extends DomainException {

    public CannotDisableOwnAccountException() {
        super("CANNOT_DISABLE_OWN_ACCOUNT", "You cannot disable your own account");
    }
}
