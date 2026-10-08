package com.lavarapido.security.domain.port.out;

import com.lavarapido.security.domain.model.Permission;
import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.model.RolePermissions;

import java.util.List;

public interface RolePermissionRepository {

    List<Permission> findPermissions();

    List<RolePermissions> findRolePermissions();

    RolePermissions findRolePermissions(RoleCode role);

    /** Deja exactamente esos permisos para el rol (quita los que ya no estén, agrega los nuevos). */
    RolePermissions replacePermissions(RoleCode role, List<Short> permissionIds, long actor);
}
