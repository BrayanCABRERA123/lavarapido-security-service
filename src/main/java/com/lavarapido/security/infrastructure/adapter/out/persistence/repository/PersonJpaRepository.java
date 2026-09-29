package com.lavarapido.security.infrastructure.adapter.out.persistence.repository;

import com.lavarapido.security.infrastructure.adapter.out.persistence.entity.PersonJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonJpaRepository extends JpaRepository<PersonJpaEntity, Long> {

    Optional<PersonJpaEntity> findByDocumentNumber(String documentNumber);
}
