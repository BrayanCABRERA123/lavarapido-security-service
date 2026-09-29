package com.lavarapido.security.infrastructure.adapter.out.persistence.repository;

import com.lavarapido.security.infrastructure.adapter.out.persistence.entity.PasswordResetTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenJpaEntity, Long> {

    List<PasswordResetTokenJpaEntity> findByUserIdAndUsedAtIsNullAndExpiresAtAfter(Long userId, Instant now);

    Optional<PasswordResetTokenJpaEntity> findFirstByUserIdAndUsedAtIsNullAndExpiresAtAfterOrderByRequestedAtDescIdDesc(
            Long userId, Instant now);
}
