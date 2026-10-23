package com.lavarapido.security.infrastructure.adapter.out.persistence.repository;

import com.lavarapido.security.infrastructure.adapter.out.persistence.entity.LoginLockoutJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginLockoutJpaRepository extends JpaRepository<LoginLockoutJpaEntity, Long> {
}
