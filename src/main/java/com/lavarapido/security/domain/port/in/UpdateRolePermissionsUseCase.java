package com.lavarapido.security.domain.port.in;

import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.model.RolePermissions;

import java.util.List;

/** Reemplaza los permisos de uno de los 3 roles fijos (ADMIN/OPERATOR/CLIENT). */
public interface UpdateRolePermissionsUseCase {

    RolePermissions updateRolePermissions(RoleCode role, List<Short> permissionIds);
}
