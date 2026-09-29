package com.lavarapido.security.infrastructure.adapter.out.persistence.repository;

import com.lavarapido.security.infrastructure.adapter.out.persistence.entity.AppUserJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppUserJpaRepository extends JpaRepository<AppUserJpaEntity, Long> {

    Optional<AppUserJpaEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByPerson_Id(Long personId);

    @Query("select u from AppUserJpaEntity u where exists (select r from u.roles r where r.code = :roleCode)")
    Page<AppUserJpaEntity> findAllWithRole(@Param("roleCode") String roleCode, Pageable pageable);
}
