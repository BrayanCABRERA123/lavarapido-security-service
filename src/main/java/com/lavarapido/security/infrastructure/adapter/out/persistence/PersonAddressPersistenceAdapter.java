package com.lavarapido.security.infrastructure.adapter.out.persistence;

import com.lavarapido.security.domain.port.out.PersonAddressRepository;
import com.lavarapido.security.infrastructure.adapter.out.persistence.entity.PersonAddressJpaEntity;
import com.lavarapido.security.infrastructure.adapter.out.persistence.repository.PersonAddressJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Optional;

@Component
class PersonAddressPersistenceAdapter implements PersonAddressRepository {

    private final PersonAddressJpaRepository addresses;
    private final Clock clock;

    PersonAddressPersistenceAdapter(PersonAddressJpaRepository addresses, Clock clock) {
        this.addresses = addresses;
        this.clock = clock;
    }

    @Override
    public Optional<String> findByPersonId(long personId) {
        return addresses.findById(personId).map(PersonAddressJpaEntity::getAddress);
    }

    @Override
    public void save(long personId, String address) {
        PersonAddressJpaEntity entity = addresses.findById(personId)
                .orElseGet(() -> new PersonAddressJpaEntity(personId));
        entity.setAddress(address);
        entity.setUpdatedAt(clock.instant());
        addresses.save(entity);
    }

    @Override
    public void delete(long personId) {
        if (addresses.existsById(personId)) {
            addresses.deleteById(personId);
        }
    }
}
