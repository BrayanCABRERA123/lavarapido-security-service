package com.lavarapido.security.infrastructure.adapter.out.persistence.repository;

import com.lavarapido.security.infrastructure.adapter.out.persistence.entity.UserSessionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSessionJpaRepository extends JpaRepository<UserSessionJpaEntity, Long> {

    Optional<UserSessionJpaEntity> findByIdAndUserId(Long id, Long userId);

    List<UserSessionJpaEntity> findByUserIdAndRevokedAtIsNull(Long userId);
}
