package com.lavarapido.security.infrastructure.adapter.out.persistence.repository;

import com.lavarapido.security.infrastructure.adapter.out.persistence.entity.RoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, Short> {

    List<RoleJpaEntity> findByCodeIn(Collection<String> codes);
}
