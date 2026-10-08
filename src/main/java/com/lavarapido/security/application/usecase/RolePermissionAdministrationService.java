package com.lavarapido.security.application.usecase;

import com.lavarapido.security.domain.model.Permission;
import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.model.RolePermissions;
import com.lavarapido.security.domain.port.in.ListRolePermissionsUseCase;
import com.lavarapido.security.domain.port.in.UpdateRolePermissionsUseCase;
import com.lavarapido.security.domain.port.out.RolePermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestión &gt; Roles: los 3 roles siguen fijos (ADR-010), lo único editable son sus permisos
 * (security.permission / security.role_permission, migración 026). No tiene ningún efecto sobre
 * la autorización real todavía (eso seguiría viniendo de RoleCode en el JWT); es solo lo que ve y
 * edita el admin en esa pantalla.
 */
@Service
public class RolePermissionAdministrationService implements ListRolePermissionsUseCase, UpdateRolePermissionsUseCase {

    private final RolePermissionRepository repository;

    public RolePermissionAdministrationService(RolePermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Permission> listPermissions() {
        return repository.findPermissions();
    }

    @Override
    public List<RolePermissions> listRolePermissions() {
        return repository.findRolePermissions();
    }

    @Override
    @Transactional
    public RolePermissions updateRolePermissions(RoleCode role, List<Short> permissionIds, long actor) {
        return repository.replacePermissions(role, permissionIds, actor);
    }
}
