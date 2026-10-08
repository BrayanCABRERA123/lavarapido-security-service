package com.lavarapido.security.infrastructure.adapter.out.persistence.repository;

import com.lavarapido.security.infrastructure.adapter.out.persistence.entity.RolePermissionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionJpaRepository extends JpaRepository<RolePermissionJpaEntity, Long> {

    List<RolePermissionJpaEntity> findByRoleId(short roleId);

    void deleteByRoleId(short roleId);
}
