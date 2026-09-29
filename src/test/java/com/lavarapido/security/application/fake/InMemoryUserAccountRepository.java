package com.lavarapido.security.application.fake;

import com.lavarapido.security.domain.model.DocumentNumber;
import com.lavarapido.security.domain.model.EmailAddress;
import com.lavarapido.security.domain.model.PageResult;
import com.lavarapido.security.domain.model.Person;
import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.model.UserAccount;
import com.lavarapido.security.domain.port.out.UserAccountRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Se comporta como el adaptador SQL: asigna ids, guarda personas sin cuenta y respeta la unicidad. */
public class InMemoryUserAccountRepository implements UserAccountRepository {

    private final Map<Long, UserAccount> accounts = new LinkedHashMap<>();
    private final Map<String, Person> personsByDocument = new LinkedHashMap<>();
    private long nextUserId = 1;
    private long nextPersonId = 100;

    /** Una persona registrada en el mostrador, sin cuenta. */
    public Person addWalkInPerson(Person person) {
        Person stored = Person.reconstitute(nextPersonId++, person.documentNumber(), person.name(), person.phone(),
                person.email());
        personsByDocument.put(stored.documentNumber().value(), stored);
        return stored;
    }

    @Override
    public Optional<UserAccount> findById(long userId) {
        return Optional.ofNullable(accounts.get(userId));
    }

    @Override
    public Optional<UserAccount> findByUsername(EmailAddress username) {
        return accounts.values().stream().filter(account -> account.username().equals(username)).findFirst();
    }

    @Override
    public boolean existsByUsername(EmailAddress username) {
        return findByUsername(username).isPresent();
    }

    @Override
    public Optional<Person> findPersonByDocument(DocumentNumber documentNumber) {
        return Optional.ofNullable(personsByDocument.get(documentNumber.value()));
    }

    @Override
    public boolean existsAccountForPerson(long personId) {
        return accounts.values().stream().anyMatch(account -> account.person().id() == personId);
    }

    @Override
    public UserAccount save(UserAccount account) {
        Person person = account.person();
        if (person.id() == null) {
            person = Person.reconstitute(nextPersonId++, person.documentNumber(), person.name(), person.phone(),
                    person.email());
        }
        personsByDocument.put(person.documentNumber().value(), person);

        long id = account.id() == null ? nextUserId++ : account.id();
        UserAccount stored = UserAccount.reconstitute(id, person, account.username(), account.passwordHash(),
                account.isActive(), account.lastLogin(), account.roles());
        accounts.put(id, stored);
        return stored;
    }

    @Override
    public PageResult<UserAccount> findAll(RoleCode role, int page, int size) {
        List<UserAccount> matching = new ArrayList<>(accounts.values().stream()
                .filter(account -> role == null || account.hasRole(role))
                .sorted(Comparator.comparing(UserAccount::id))
                .toList());
        int from = Math.min(page * size, matching.size());
        int to = Math.min(from + size, matching.size());
        return new PageResult<>(matching.subList(from, to), page, size, matching.size());
    }

    public int count() {
        return accounts.size();
    }
}
