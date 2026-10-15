package com.lavarapido.security.application.usecase;

import com.lavarapido.security.domain.exception.InvalidValueException;
import com.lavarapido.security.domain.exception.UserNotFoundException;
import com.lavarapido.security.domain.port.in.PersonAddressUseCase;
import com.lavarapido.security.domain.port.out.PersonAddressRepository;
import com.lavarapido.security.domain.port.out.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dirección del perfil (ADR-010): antes se guardaba solo en el navegador o el celular. */
@Service
@Transactional
public class PersonAddressService implements PersonAddressUseCase {

    private final UserAccountRepository accounts;
    private final PersonAddressRepository addresses;

    public PersonAddressService(UserAccountRepository accounts, PersonAddressRepository addresses) {
        this.accounts = accounts;
        this.addresses = addresses;
    }

    @Override
    @Transactional(readOnly = true)
    public String getAddress(long userId) {
        return addresses.findByPersonId(personIdOf(userId)).orElse("");
    }

    @Override
    public String updateAddress(long userId, String address) {
        long personId = personIdOf(userId);
        String clean = address == null ? "" : address.strip();
        if (clean.length() > MAX_LENGTH) {
            throw new InvalidValueException("INVALID_ADDRESS",
                    "The address must have at most " + MAX_LENGTH + " characters");
        }
        if (clean.isEmpty()) {
            addresses.delete(personId);
        } else {
            addresses.save(personId, clean);
        }
        return clean;
    }

    private long personIdOf(long userId) {
        return accounts.findById(userId).orElseThrow(UserNotFoundException::new).person().id();
    }
}
