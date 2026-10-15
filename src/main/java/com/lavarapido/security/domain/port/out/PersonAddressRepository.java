package com.lavarapido.security.domain.port.out;

import java.util.Optional;

/** Dirección de contacto de cada persona (security.person_address, migración 035, ADR-010). */
public interface PersonAddressRepository {

    Optional<String> findByPersonId(long personId);

    void save(long personId, String address);

    void delete(long personId);
}
