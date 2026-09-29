package com.lavarapido.security.domain.port.out;

import com.lavarapido.security.domain.model.DocumentNumber;
import com.lavarapido.security.domain.model.EmailAddress;
import com.lavarapido.security.domain.model.PageResult;
import com.lavarapido.security.domain.model.Person;
import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.model.UserAccount;

import java.util.Optional;

public interface UserAccountRepository {

    Optional<UserAccount> findById(long userId);

    Optional<UserAccount> findByUsername(EmailAddress username);

    boolean existsByUsername(EmailAddress username);

    /** Una persona puede existir sin cuenta (cliente registrado en el mostrador). */
    Optional<Person> findPersonByDocument(DocumentNumber documentNumber);

    boolean existsAccountForPerson(long personId);

    /** Guarda la cuenta con su persona y roles; la devuelve con los ids generados. */
    UserAccount save(UserAccount account);

    /** @param role filtro opcional; {@code null} significa todos los roles */
    PageResult<UserAccount> findAll(RoleCode role, int page, int size);
}
