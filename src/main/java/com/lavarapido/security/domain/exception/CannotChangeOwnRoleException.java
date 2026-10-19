package com.lavarapido.security.domain.exception;

/** Un administrador intentó cambiar el rol de su propia cuenta (se quedaría sin acceso a Gestión). */
public class CannotChangeOwnRoleException extends DomainException {

    public CannotChangeOwnRoleException() {
        super("CANNOT_CHANGE_OWN_ROLE", "You cannot change the role of your own account");
    }
}
