package com.lavarapido.security.application.usecase;

import com.lavarapido.security.domain.event.UserRegistered;
import com.lavarapido.security.domain.exception.DocumentAlreadyRegisteredException;
import com.lavarapido.security.domain.exception.EmailAlreadyRegisteredException;
import com.lavarapido.security.domain.exception.InvalidValueException;
import com.lavarapido.security.domain.model.DocumentNumber;
import com.lavarapido.security.domain.model.EmailAddress;
import com.lavarapido.security.domain.model.Person;
import com.lavarapido.security.domain.model.PersonName;
import com.lavarapido.security.domain.model.PhoneNumber;
import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.model.UserAccount;
import com.lavarapido.security.domain.port.in.CreateUserAccountUseCase;
import com.lavarapido.security.domain.port.in.RegisterUserUseCase;
import com.lavarapido.security.domain.port.in.UserAccountView;
import com.lavarapido.security.domain.port.out.DomainEventPublisher;
import com.lavarapido.security.domain.port.out.PasswordHasher;
import com.lavarapido.security.domain.port.out.UserAccountRepository;
import com.lavarapido.security.domain.service.PasswordPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Set;

/**
 * Abre cuentas nuevas: clientes que se registran solos y cuentas que crea un administrador
 * con roles explícitos. Ambos caminos comparten las mismas reglas, por eso van juntos.
 */
@Service
@Transactional
public class AccountRegistrationService implements RegisterUserUseCase, CreateUserAccountUseCase {

    private final UserAccountRepository accounts;
    private final PasswordHasher passwordHasher;
    private final PasswordPolicy passwordPolicy;
    private final DomainEventPublisher events;
    private final Clock clock;

    public AccountRegistrationService(UserAccountRepository accounts, PasswordHasher passwordHasher,
                                      PasswordPolicy passwordPolicy, DomainEventPublisher events, Clock clock) {
        this.accounts = accounts;
        this.passwordHasher = passwordHasher;
        this.passwordPolicy = passwordPolicy;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public UserAccountView register(RegisterUserCommand command) {
        UserAccount account = open(command.documentNumber(), command.firstName(), command.lastName(),
                command.email(), command.phone(), command.password(), Set.of(RoleCode.CLIENT), false);
        return UserAccountView.from(account);
    }

    @Override
    public UserAccountView createAccount(CreateUserAccountCommand command) {
        if (command.roles() == null || command.roles().isEmpty()) {
            throw new InvalidValueException("INVALID_ROLES", "At least one role is required");
        }
        UserAccount account = open(command.documentNumber(), command.firstName(), command.lastName(),
                command.email(), command.phone(), command.password(), command.roles(), true);
        return UserAccountView.from(account);
    }

    /** @param createdByAdmin la abre un administrador (la persona no eligió su contraseña) */
    private UserAccount open(String rawDocument, String firstName, String lastName, String rawEmail,
                             String rawPhone, String rawPassword, Set<RoleCode> roles, boolean createdByAdmin) {
        DocumentNumber document = DocumentNumber.of(rawDocument);
        PersonName name = PersonName.of(firstName, lastName);
        EmailAddress email = EmailAddress.of(rawEmail);
        PhoneNumber phone = PhoneNumber.ofNullable(rawPhone);
        passwordPolicy.validate(rawPassword);

        if (accounts.existsByUsername(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        Person person = accounts.findPersonByDocument(document)
                .map(existing -> reuseWalkInPerson(existing, phone, email))
                .orElseGet(() -> Person.create(document, name, phone, email));

        UserAccount account = UserAccount.create(person, email, passwordHasher.hash(rawPassword), roles);
        UserAccount saved = accounts.save(account);

        // El evento sale después del commit (cross-cutting.md §6): con MESSAGING_ENABLED=true lo
        // publica RabbitMQ en afterCommit; si no, el adaptador de log solo lo anota.
        events.publish(UserRegistered.of(saved, createdByAdmin, clock.instant()));
        return saved;
    }

    /**
     * Una persona registrada en el mostrador (sin cuenta) puede luego registrarse en línea con el mismo
     * documento. Mandan los datos del mostrador; solo se completan los datos de contacto que falten.
     */
    private Person reuseWalkInPerson(Person existing, PhoneNumber phone, EmailAddress email) {
        if (accounts.existsAccountForPerson(existing.id())) {
            throw new DocumentAlreadyRegisteredException();
        }
        existing.completeContactInfo(phone, email);
        return existing;
    }
}
