package com.lavarapido.security.domain.exception;

/** Una cuenta de administrador no se desactiva: primero hay que cambiarle el rol. */
public class AdminAccountCannotBeDisabledException extends DomainException {

    public AdminAccountCannotBeDisabledException() {
        super("ADMIN_CANNOT_BE_DISABLED", "An administrator account cannot be disabled; change its role first");
    }
}
