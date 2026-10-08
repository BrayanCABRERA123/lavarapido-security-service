package com.lavarapido.security.domain.port.in;

import com.lavarapido.security.domain.model.Permission;
import com.lavarapido.security.domain.model.RolePermissions;

import java.util.List;

/** Gestión &gt; Roles: el catálogo de permisos y qué tiene asignado cada uno de los 3 roles fijos. */
public interface ListRolePermissionsUseCase {

    List<Permission> listPermissions();

    List<RolePermissions> listRolePermissions();
}
