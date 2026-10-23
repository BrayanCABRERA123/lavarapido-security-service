package com.lavarapido.security.domain.exception;

import java.util.Collection;

/** Se pidió asignar a un rol un permiso que no está en el catálogo (security.permission). */
public class UnknownPermissionException extends DomainException {

    public UnknownPermissionException(Collection<Short> unknownIds) {
        super("UNKNOWN_PERMISSION", "Unknown permission ids: " + unknownIds);
    }
}
