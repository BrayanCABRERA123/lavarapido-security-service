package com.lavarapido.security.infrastructure.adapter.out.persistence;

import com.lavarapido.security.domain.model.Permission;
import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.model.RolePermissions;
import com.lavarapido.security.domain.port.out.RolePermissionRepository;
import com.lavarapido.security.infrastructure.adapter.out.persistence.entity.PermissionJpaEntity;
import com.lavarapido.security.infrastructure.adapter.out.persistence.entity.RoleJpaEntity;
import com.lavarapido.security.infrastructure.adapter.out.persistence.entity.RolePermissionJpaEntity;
import com.lavarapido.security.infrastructure.adapter.out.persistence.repository.PermissionJpaRepository;
import com.lavarapido.security.infrastructure.adapter.out.persistence.repository.RoleJpaRepository;
import com.lavarapido.security.infrastructure.adapter.out.persistence.repository.RolePermissionJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
class RolePermissionPersistenceAdapter implements RolePermissionRepository {

    private final RoleJpaRepository roles;
    private final PermissionJpaRepository permissions;
    private final RolePermissionJpaRepository rolePermissions;

    RolePermissionPersistenceAdapter(RoleJpaRepository roles, PermissionJpaRepository permissions,
                                     RolePermissionJpaRepository rolePermissions) {
        this.roles = roles;
        this.permissions = permissions;
        this.rolePermissions = rolePermissions;
    }

    @Override
    public List<Permission> findPermissions() {
        return permissions.findAll().stream()
                .sorted(Comparator.comparing(PermissionJpaEntity::getId))
                .map(RolePermissionPersistenceAdapter::toPermission)
                .toList();
    }

    @Override
    public List<RolePermissions> findRolePermissions() {
        Map<Short, List<Short>> byRole = rolePermissions.findAll().stream()
                .collect(Collectors.groupingBy(RolePermissionJpaEntity::getRoleId,
                        Collectors.mapping(RolePermissionJpaEntity::getPermissionId, Collectors.toList())));
        return roles.findByCodeIn(codesOf(RoleCode.values())).stream()
                .map(role -> new RolePermissions(RoleCode.valueOf(role.getCode()),
                        byRole.getOrDefault(role.getId(), List.of())))
                .toList();
    }

    @Override
    public RolePermissions findRolePermissions(RoleCode role) {
        RoleJpaEntity entity = roleEntityOf(role);
        List<Short> permissionIds = rolePermissions.findByRoleId(entity.getId()).stream()
                .map(RolePermissionJpaEntity::getPermissionId)
                .toList();
        return new RolePermissions(role, permissionIds);
    }

    @Override
    public RolePermissions replacePermissions(RoleCode role, List<Short> permissionIds) {
        RoleJpaEntity entity = roleEntityOf(role);
        rolePermissions.deleteByRoleId(entity.getId());
        rolePermissions.flush();
        for (Short permissionId : permissionIds) {
            rolePermissions.save(new RolePermissionJpaEntity(entity.getId(), permissionId));
        }
        return new RolePermissions(role, permissionIds);
    }

    // los 3 roles siempre están sembrados (migración seed/101): si falta uno, el seed no corrió
    private RoleJpaEntity roleEntityOf(RoleCode role) {
        return roles.findByCodeIn(List.of(role.name())).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("El rol " + role + " no está sembrado en security.role"));
    }

    private static List<String> codesOf(RoleCode[] values) {
        return List.of(values).stream().map(Enum::name).toList();
    }

    private static Permission toPermission(PermissionJpaEntity entity) {
        return new Permission(entity.getId(), entity.getCode(), entity.getName(), entity.getResource(), entity.getAction());
    }
}
