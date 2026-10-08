package com.lavarapido.security.application.usecase;

import com.lavarapido.security.domain.exception.UnknownPermissionException;
import com.lavarapido.security.domain.model.Permission;
import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.model.RolePermissions;
import com.lavarapido.security.domain.port.in.ListRolePermissionsUseCase;
import com.lavarapido.security.domain.port.in.UpdateRolePermissionsUseCase;
import com.lavarapido.security.domain.port.out.RolePermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    /**
     * Deja exactamente esos permisos al rol. Un id que no está en el catálogo se rechaza (antes se
     * descartaba en silencio y el admin creía haberlo guardado); los repetidos cuentan una vez.
     */
    @Override
    @Transactional
    public RolePermissions updateRolePermissions(RoleCode role, List<Short> permissionIds) {
        Set<Short> catalog = repository.findPermissions().stream().map(Permission::id).collect(Collectors.toSet());
        List<Short> wanted = permissionIds.stream().distinct().toList();
        List<Short> unknown = wanted.stream().filter(id -> !catalog.contains(id)).toList();
        if (!unknown.isEmpty()) {
            throw new UnknownPermissionException(unknown);
        }
        return repository.replacePermissions(role, wanted);
    }
}
