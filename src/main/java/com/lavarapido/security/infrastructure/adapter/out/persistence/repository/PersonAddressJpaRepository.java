package com.lavarapido.security.infrastructure.adapter.out.persistence.repository;

import com.lavarapido.security.infrastructure.adapter.out.persistence.entity.PersonAddressJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonAddressJpaRepository extends JpaRepository<PersonAddressJpaEntity, Long> {
}
