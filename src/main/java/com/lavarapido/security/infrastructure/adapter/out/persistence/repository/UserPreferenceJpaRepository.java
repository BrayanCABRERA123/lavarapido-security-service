package com.lavarapido.security.infrastructure.adapter.out.persistence.repository;

import com.lavarapido.security.infrastructure.adapter.out.persistence.entity.UserPreferenceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPreferenceJpaRepository extends JpaRepository<UserPreferenceJpaEntity, Long> {

    Optional<UserPreferenceJpaEntity> findByUserId(Long userId);
}
