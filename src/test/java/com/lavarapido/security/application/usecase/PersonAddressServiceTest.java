package com.lavarapido.security.application.usecase;

import com.lavarapido.security.domain.model.AllowedEmailDomains;
import com.lavarapido.security.application.fake.Fakes.MutableClock;
import com.lavarapido.security.application.fake.Fakes.PlainPasswordHasher;
import com.lavarapido.security.application.fake.Fakes.RecordingEventPublisher;
import com.lavarapido.security.application.fake.InMemoryUserAccountRepository;
import com.lavarapido.security.domain.exception.InvalidValueException;
import com.lavarapido.security.domain.exception.UserNotFoundException;
import com.lavarapido.security.domain.port.in.RegisterUserUseCase.RegisterUserCommand;
import com.lavarapido.security.domain.port.out.PersonAddressRepository;
import com.lavarapido.security.domain.service.PasswordPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonAddressServiceTest {

    private final InMemoryUserAccountRepository accounts = new InMemoryUserAccountRepository();
    private final InMemoryAddresses addresses = new InMemoryAddresses();
    private final PersonAddressService service = new PersonAddressService(accounts, addresses);

    private long anaId;

    @BeforeEach
    void registerAna() {
        anaId = new AccountRegistrationService(accounts, new PlainPasswordHasher(), new PasswordPolicy(),
                new RecordingEventPublisher(), new MutableClock(Instant.parse("2026-10-15T15:00:00Z")), AllowedEmailDomains.parse("gmail.com"))
                .register(new RegisterUserCommand("1023456789", "Ana", "Pérez", "ana@gmail.com", null, "Lavado2026!"))
                .id();
    }

    @Test
    void withoutAnAddressItIsEmpty() {
        assertThat(service.getAddress(anaId)).isEmpty();
    }

    @Test
    void theAddressIsSavedTrimmedAndCanBeChanged() {
        assertThat(service.updateAddress(anaId, "  Calle 127 #19A-48  ")).isEqualTo("Calle 127 #19A-48");
        service.updateAddress(anaId, "Carrera 7 #45-10");

        assertThat(service.getAddress(anaId)).isEqualTo("Carrera 7 #45-10");
        assertThat(addresses.rows).hasSize(1);
    }

    @Test
    void anEmptyAddressDeletesIt() {
        service.updateAddress(anaId, "Calle 127 #19A-48");

        assertThat(service.updateAddress(anaId, "   ")).isEmpty();
        assertThat(addresses.rows).isEmpty();
    }

    @Test
    void aTooLongAddressIsRejected() {
        assertThatThrownBy(() -> service.updateAddress(anaId, "x".repeat(201)))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void anUnknownUserHasNoAddress() {
        assertThatThrownBy(() -> service.getAddress(999L)).isInstanceOf(UserNotFoundException.class);
    }

    private static final class InMemoryAddresses implements PersonAddressRepository {

        private final Map<Long, String> rows = new HashMap<>();

        @Override
        public Optional<String> findByPersonId(long personId) {
            return Optional.ofNullable(rows.get(personId));
        }

        @Override
        public void save(long personId, String address) {
            rows.put(personId, address);
        }

        @Override
        public void delete(long personId) {
            rows.remove(personId);
        }
    }
}
