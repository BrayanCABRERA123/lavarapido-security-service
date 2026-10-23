package com.lavarapido.security.infrastructure.adapter.in.web.dto;

import com.lavarapido.security.domain.model.Permission;
import com.lavarapido.security.domain.model.RolePermissions;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Contratos HTTP de Gestión &gt; Roles (/api/v1/admin/roles-permissions). */
public final class RolePermissionDtos {

    private RolePermissionDtos() {
    }

    public record PermissionResponse(short id, String code, String name, String resource, String action) {

        public static PermissionResponse from(Permission p) {
            return new PermissionResponse(p.id(), p.code(), p.name(), p.resource(), p.action());
        }
    }

    public record RolePermissionsResponse(String role, List<Short> permissionIds) {

        public static RolePermissionsResponse from(RolePermissions rp) {
            return new RolePermissionsResponse(rp.role().name(), rp.permissionIds());
        }
    }

    /** permissions: el catálogo completo; roles: lo que tiene asignado cada uno de los 3 roles fijos. */
    public record RolePermissionsMatrixResponse(List<PermissionResponse> permissions, List<RolePermissionsResponse> roles) {
    }

    // sin nulls adentro: [null] antes dejaba el rol sin ningún permiso y respondía 200
    public record UpdateRolePermissionsRequest(@NotNull List<@NotNull Short> permissionIds) {
    }
}
