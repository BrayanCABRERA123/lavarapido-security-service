package com.lavarapido.security.infrastructure.adapter.out.persistence;

import com.lavarapido.security.domain.model.DocumentNumber;
import com.lavarapido.security.domain.model.EmailAddress;
import com.lavarapido.security.domain.model.HashedPassword;
import com.lavarapido.security.domain.model.PageResult;
import com.lavarapido.security.domain.model.Person;
import com.lavarapido.security.domain.model.PersonName;
import com.lavarapido.security.domain.model.PhoneNumber;
import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.model.UserAccount;
import com.lavarapido.security.domain.port.out.UserAccountRepository;
import com.lavarapido.security.infrastructure.adapter.out.persistence.entity.AppUserJpaEntity;
import com.lavarapido.security.infrastructure.adapter.out.persistence.entity.PersonJpaEntity;
import com.lavarapido.security.infrastructure.adapter.out.persistence.entity.RoleJpaEntity;
import com.lavarapido.security.infrastructure.adapter.out.persistence.repository.AppUserJpaRepository;
import com.lavarapido.security.infrastructure.adapter.out.persistence.repository.PersonJpaRepository;
import com.lavarapido.security.infrastructure.adapter.out.persistence.repository.RoleJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Mapea el agregado {@link UserAccount} a {@code person}, {@code app_user} y {@code user_role}. */
@Component
class UserAccountPersistenceAdapter implements UserAccountRepository {

    private final AppUserJpaRepository users;
    private final PersonJpaRepository persons;
    private final RoleJpaRepository roles;

    UserAccountPersistenceAdapter(AppUserJpaRepository users, PersonJpaRepository persons, RoleJpaRepository roles) {
        this.users = users;
        this.persons = persons;
        this.roles = roles;
    }

    @Override
    public Optional<UserAccount> findById(long userId) {
        return users.findById(userId).map(UserAccountPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<UserAccount> findByUsername(EmailAddress username) {
        return users.findByUsername(username.value()).map(UserAccountPersistenceAdapter::toDomain);
    }

    @Override
    public boolean existsByUsername(EmailAddress username) {
        return users.existsByUsername(username.value());
    }

    @Override
    public Optional<Person> findPersonByDocument(DocumentNumber documentNumber) {
        return persons.findByDocumentNumber(documentNumber.value()).map(UserAccountPersistenceAdapter::toDomain);
    }

    @Override
    public boolean existsAccountForPerson(long personId) {
        return users.existsByPerson_Id(personId);
    }

    @Override
    public UserAccount save(UserAccount account) {
        AppUserJpaEntity entity = account.id() == null
                ? new AppUserJpaEntity(personEntityFor(account.person()), account.username().value())
                : users.findById(account.id()).orElseThrow(
                        () -> new IllegalStateException("Account " + account.id() + " vanished while being updated"));

        copyPerson(account.person(), entity.getPerson());
        entity.setUsername(account.username().value());
        entity.setPasswordHash(account.passwordHash().value());
        entity.setActive(account.isActive());
        entity.setLastLogin(account.lastLogin());
        entity.replaceRoles(roleEntities(account.roles()));

        return toDomain(users.save(entity));
    }

    @Override
    public PageResult<UserAccount> findAll(RoleCode role, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("id"));
        Page<AppUserJpaEntity> result = role == null
                ? users.findAll(pageRequest)
                : users.findAllWithRole(role.name(), pageRequest);
        List<UserAccount> items = result.getContent().stream().map(UserAccountPersistenceAdapter::toDomain).toList();
        return new PageResult<>(items, page, size, result.getTotalElements());
    }

    private PersonJpaEntity personEntityFor(Person person) {
        if (person.id() == null) {
            return new PersonJpaEntity(person.documentNumber().value());
        }
        return persons.findById(person.id()).orElseThrow(
                () -> new IllegalStateException("Person " + person.id() + " vanished while being linked"));
    }

    private Set<RoleJpaEntity> roleEntities(Set<RoleCode> codes) {
        Set<String> names = codes.stream().map(RoleCode::name).collect(Collectors.toSet());
        Set<RoleJpaEntity> found = new HashSet<>(roles.findByCodeIn(names));
        if (found.size() != names.size()) {
            throw new IllegalStateException("Roles " + names + " are not all seeded in security.role");
        }
        return found;
    }

    private static void copyPerson(Person person, PersonJpaEntity entity) {
        entity.setFirstName(person.name().firstName());
        entity.setLastName(person.name().lastName());
        entity.setPhone(person.phone() == null ? null : person.phone().value());
        entity.setEmail(person.email() == null ? null : person.email().value());
    }

    private static UserAccount toDomain(AppUserJpaEntity entity) {
        Set<RoleCode> roleCodes = entity.getRoles().stream()
                .map(role -> RoleCode.valueOf(role.getCode()))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(RoleCode.class)));
        return UserAccount.reconstitute(
                entity.getId(),
                toDomain(entity.getPerson()),
                EmailAddress.of(entity.getUsername()),
                new HashedPassword(entity.getPasswordHash()),
                entity.isActive(),
                entity.getLastLogin(),
                roleCodes);
    }

    private static Person toDomain(PersonJpaEntity entity) {
        return Person.reconstitute(
                entity.getId(),
                DocumentNumber.of(entity.getDocumentNumber()),
                PersonName.of(entity.getFirstName(), entity.getLastName()),
                PhoneNumber.ofNullable(entity.getPhone()),
                entity.getEmail() == null ? null : EmailAddress.of(entity.getEmail()));
    }
}
