package com.lavarapido.security.infrastructure.adapter.in.web;

import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.port.in.ListRolePermissionsUseCase;
import com.lavarapido.security.domain.port.in.UpdateRolePermissionsUseCase;
import com.lavarapido.security.infrastructure.adapter.in.web.dto.RolePermissionDtos.PermissionResponse;
import com.lavarapido.security.infrastructure.adapter.in.web.dto.RolePermissionDtos.RolePermissionsMatrixResponse;
import com.lavarapido.security.infrastructure.adapter.in.web.dto.RolePermissionDtos.RolePermissionsResponse;
import com.lavarapido.security.infrastructure.adapter.in.web.dto.RolePermissionDtos.UpdateRolePermissionsRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestión &gt; Roles: los 3 roles son fijos (ADMIN/OPERATOR/CLIENT, ADR-010); lo único que el
 * admin edita aquí son los permisos de cada uno. {@code /api/v1/admin/**} exige el rol ADMIN
 * (SecurityConfig).
 */
@Tag(name = "Admin - roles y permisos", description = "Permisos de los 3 roles fijos (solo ADMIN)")
@RestController
@RequestMapping("/api/v1/admin/roles-permissions")
class RolePermissionController {

    private final ListRolePermissionsUseCase listRolePermissions;
    private final UpdateRolePermissionsUseCase updateRolePermissions;

    RolePermissionController(ListRolePermissionsUseCase listRolePermissions,
                             UpdateRolePermissionsUseCase updateRolePermissions) {
        this.listRolePermissions = listRolePermissions;
        this.updateRolePermissions = updateRolePermissions;
    }

    @GetMapping
    RolePermissionsMatrixResponse matrix() {
        var permissions = listRolePermissions.listPermissions().stream().map(PermissionResponse::from).toList();
        var roles = listRolePermissions.listRolePermissions().stream().map(RolePermissionsResponse::from).toList();
        return new RolePermissionsMatrixResponse(permissions, roles);
    }

    @PutMapping("/{role}")
    RolePermissionsResponse update(@PathVariable RoleCode role, @Valid @RequestBody UpdateRolePermissionsRequest request) {
        return RolePermissionsResponse.from(updateRolePermissions.updateRolePermissions(role, request.permissionIds()));
    }
}
