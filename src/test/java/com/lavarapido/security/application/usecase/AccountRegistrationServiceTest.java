package com.lavarapido.security.application.usecase;

import com.lavarapido.security.application.fake.Fakes.MutableClock;
import com.lavarapido.security.application.fake.Fakes.PlainPasswordHasher;
import com.lavarapido.security.application.fake.Fakes.RecordingEventPublisher;
import com.lavarapido.security.application.fake.InMemoryUserAccountRepository;
import com.lavarapido.security.domain.event.UserRegistered;
import com.lavarapido.security.domain.exception.DocumentAlreadyRegisteredException;
import com.lavarapido.security.domain.exception.EmailAlreadyRegisteredException;
import com.lavarapido.security.domain.exception.InvalidValueException;
import com.lavarapido.security.domain.exception.WeakPasswordException;
import com.lavarapido.security.domain.model.DocumentNumber;
import com.lavarapido.security.domain.model.EmailAddress;
import com.lavarapido.security.domain.model.Person;
import com.lavarapido.security.domain.model.PersonName;
import com.lavarapido.security.domain.model.PhoneNumber;
import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.model.UserAccount;
import com.lavarapido.security.domain.port.in.CreateUserAccountUseCase.CreateUserAccountCommand;
import com.lavarapido.security.domain.port.in.RegisterUserUseCase.RegisterUserCommand;
import com.lavarapido.security.domain.port.in.UserAccountView;
import com.lavarapido.security.domain.service.PasswordPolicy;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountRegistrationServiceTest {

    private final InMemoryUserAccountRepository accounts = new InMemoryUserAccountRepository();
    private final RecordingEventPublisher events = new RecordingEventPublisher();
    private final AccountRegistrationService service = new AccountRegistrationService(accounts,
            new PlainPasswordHasher(), new PasswordPolicy(), events,
            new MutableClock(Instant.parse("2026-09-29T15:00:00Z")));

    private static RegisterUserCommand ana() {
        return new RegisterUserCommand("1023456789", "Ana", "Pérez", "Ana@Gmail.com", "3001234567", "Lavado2026!");
    }

    @Test
    void registersACustomerWithHashedPasswordAndPublishesTheEvent() {
        UserAccountView view = service.register(ana());

        assertThat(view.email()).isEqualTo("ana@gmail.com");
        assertThat(view.roles()).containsExactly(RoleCode.CLIENT);
        UserAccount stored = accounts.findById(view.id()).orElseThrow();
        assertThat(stored.passwordHash().value()).isEqualTo("hashed:Lavado2026!");
        assertThat(events.events).singleElement().isInstanceOf(UserRegistered.class);
    }

    @Test
    void theSameEmailCannotOpenTwoAccounts() {
        service.register(ana());

        RegisterUserCommand sameEmail = new RegisterUserCommand("99999999", "Otra", "Persona", "ana@gmail.com",
                null, "Lavado2026!");

        assertThatThrownBy(() -> service.register(sameEmail)).isInstanceOf(EmailAlreadyRegisteredException.class);
        assertThat(accounts.count()).isEqualTo(1);
    }

    @Test
    void theSamePersonCannotOpenTwoAccounts() {
        service.register(ana());

        RegisterUserCommand sameDocument = new RegisterUserCommand("1.023.456.789", "Ana", "Pérez",
                "otra@gmail.com", null, "Lavado2026!");

        assertThatThrownBy(() -> service.register(sameDocument))
                .isInstanceOf(DocumentAlreadyRegisteredException.class);
    }

    @Test
    void aWalkInCustomerCanLaterSignUpOnlineKeepingCounterData() {
        Person walkIn = accounts.addWalkInPerson(Person.create(DocumentNumber.of("1023456789"),
                PersonName.of("Ana María", "Pérez Gómez"), PhoneNumber.ofNullable("6041234567"), null));

        UserAccountView view = service.register(ana());

        UserAccount stored = accounts.findById(view.id()).orElseThrow();
        assertThat(stored.person().id()).isEqualTo(walkIn.id());
        assertThat(view.firstName()).isEqualTo("Ana María");
        assertThat(view.phone()).isEqualTo("6041234567");
        assertThat(stored.person().email()).isEqualTo(EmailAddress.of("ana@gmail.com"));
    }

    @Test
    void aWeakPasswordIsRejectedBeforeAnythingIsStored() {
        RegisterUserCommand weak = new RegisterUserCommand("1023456789", "Ana", "Pérez", "ana@gmail.com",
                null, "lavado");

        assertThatThrownBy(() -> service.register(weak)).isInstanceOf(WeakPasswordException.class);
        assertThat(accounts.count()).isZero();
        assertThat(events.events).isEmpty();
    }

    @Test
    void anAdministratorCreatesAnOperatorWithExplicitRoles() {
        UserAccountView view = service.createAccount(new CreateUserAccountCommand("80000001", "Luis", "Gómez",
                "luis@gmail.com", "3009998877", "Operario2026!", Set.of(RoleCode.OPERATOR)));

        assertThat(view.roles()).containsExactly(RoleCode.OPERATOR);
    }

    @Test
    void anAccountCreatedByAnAdministratorNeedsARole() {
        CreateUserAccountCommand noRoles = new CreateUserAccountCommand("80000001", "Luis", "Gómez",
                "luis@gmail.com", null, "Operario2026!", Set.of());

        assertThatThrownBy(() -> service.createAccount(noRoles))
                .isInstanceOf(InvalidValueException.class)
                .extracting("code").isEqualTo("INVALID_ROLES");
    }
}
