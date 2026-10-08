package com.lavarapido.security.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Catálogo de solo lectura sembrado por Liquibase (migración 026). */
@Entity
@Table(schema = "security", name = "permission")
public class PermissionJpaEntity {

    @Id
    @Column(name = "permission_id")
    private Short id;

    @Column(name = "code", insertable = false, updatable = false)
    private String code;

    @Column(name = "name", insertable = false, updatable = false)
    private String name;

    @Column(name = "resource", insertable = false, updatable = false)
    private String resource;

    @Column(name = "action", insertable = false, updatable = false)
    private String action;

    protected PermissionJpaEntity() {
    }

    public Short getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getResource() {
        return resource;
    }

    public String getAction() {
        return action;
    }
}
