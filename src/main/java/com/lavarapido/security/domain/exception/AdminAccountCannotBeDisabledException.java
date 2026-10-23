package com.lavarapido.security.domain.exception;

/** Una cuenta de administrador no se desactiva (el lavadero no puede quedar sin gestión). */
public class AdminAccountCannotBeDisabledException extends DomainException {

    public AdminAccountCannotBeDisabledException() {
        super("ADMIN_CANNOT_BE_DISABLED", "An administrator account cannot be disabled");
    }
}
