package com.lavarapido.security.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Dirección de contacto de una persona (security.person_address, ADR-010). */
@Entity
@Table(schema = "security", name = "person_address")
public class PersonAddressJpaEntity {

    @Id
    @Column(name = "person_id")
    private Long personId;

    @Column(name = "address")
    private String address;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected PersonAddressJpaEntity() {
    }

    public PersonAddressJpaEntity(Long personId) {
        this.personId = personId;
    }

    public Long getPersonId() {
        return personId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
