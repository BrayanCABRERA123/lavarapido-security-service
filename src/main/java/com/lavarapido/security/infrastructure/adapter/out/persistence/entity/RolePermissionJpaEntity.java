package com.lavarapido.security.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Una fila de security.role_permission: el permiso que tiene asignado un rol. */
@Entity
@Table(schema = "security", name = "role_permission")
public class RolePermissionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_permission_id")
    private Long id;

    @Column(name = "role_id")
    private Short roleId;

    @Column(name = "permission_id")
    private Short permissionId;

    protected RolePermissionJpaEntity() {
    }

    public RolePermissionJpaEntity(Short roleId, Short permissionId) {
        this.roleId = roleId;
        this.permissionId = permissionId;
    }

    public Long getId() {
        return id;
    }

    public Short getRoleId() {
        return roleId;
    }

    public Short getPermissionId() {
        return permissionId;
    }
}
