package com.lavarapido.security.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Catálogo de solo lectura sembrado por Liquibase ({@code ADMIN}, {@code OPERATOR}, {@code CLIENT}). */
@Entity
@Table(schema = "security", name = "`role`")
public class RoleJpaEntity {

    @Id
    @Column(name = "role_id")
    private Short id;

    @Column(name = "code", nullable = false, length = 30, insertable = false, updatable = false)
    private String code;

    @Column(name = "name", nullable = false, length = 60, insertable = false, updatable = false)
    private String name;

    protected RoleJpaEntity() {
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
}
