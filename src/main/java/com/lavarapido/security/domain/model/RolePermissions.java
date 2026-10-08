package com.lavarapido.security.domain.model;

import java.util.List;

/**
 * Los permisos asignados a uno de los 3 roles fijos (security.role_permission). Los roles en sí
 * son fijos (ADR-010); esto solo dice qué permisos tiene cada uno, y es lo que el admin edita en
 * Gestión &gt; Roles.
 */
public record RolePermissions(RoleCode role, List<Short> permissionIds) {

    public RolePermissions {
        permissionIds = List.copyOf(permissionIds);
    }
}
