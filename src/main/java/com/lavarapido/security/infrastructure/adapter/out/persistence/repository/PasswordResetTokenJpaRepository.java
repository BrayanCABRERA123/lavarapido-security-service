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

    /** El último código pedido, se haya usado o no (SEC-05: saber si el último se quemó por intentos). */
    Optional<PasswordResetTokenJpaEntity> findFirstByUserIdOrderByRequestedAtDescIdDesc(Long userId);
}
