package com.lavarapido.security.domain.model;

import com.lavarapido.security.domain.exception.AccountDisabledException;
import com.lavarapido.security.domain.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserAccountTest {

    private static final Instant NOW = Instant.parse("2026-09-29T15:00:00Z");

    private static Person person() {
        return Person.create(DocumentNumber.of("1023456789"), PersonName.of("Ana", "Pérez"),
                PhoneNumber.ofNullable("3001234567"), EmailAddress.of("ana@gmail.com"));
    }

    @Test
    void selfRegisteredAccountIsAnActiveCustomer() {
        UserAccount account = UserAccount.registerCustomer(person(), EmailAddress.of("ana@gmail.com"),
                new HashedPassword("hash"));

        assertThat(account.isActive()).isTrue();
        assertThat(account.roles()).containsExactly(RoleCode.CLIENT);
        assertThat(account.lastLogin()).isNull();
    }

    @Test
    void anAccountWithoutRolesCannotExist() {
        assertThatThrownBy(() -> UserAccount.create(person(), EmailAddress.of("ana@gmail.com"),
                new HashedPassword("hash"), Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void usernameMustFitTheLoginColumn() {
        EmailAddress longEmail = EmailAddress.of("a".repeat(55) + "@gmail.com");

        assertThatThrownBy(() -> UserAccount.registerCustomer(person(), longEmail, new HashedPassword("hash")))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void recordsTheLoginTime() {
        UserAccount account = UserAccount.registerCustomer(person(), EmailAddress.of("ana@gmail.com"),
                new HashedPassword("hash"));

        account.recordSuccessfulLogin(NOW);

        assertThat(account.lastLogin()).isEqualTo(NOW);
    }

    @Test
    void aDisabledAccountCannotLogIn() {
        UserAccount account = UserAccount.registerCustomer(person(), EmailAddress.of("ana@gmail.com"),
                new HashedPassword("hash"));
        account.deactivate();

        assertThatThrownBy(() -> account.recordSuccessfulLogin(NOW)).isInstanceOf(AccountDisabledException.class);
        assertThat(account.lastLogin()).isNull();
    }

    @Test
    void rolesCannotBeModifiedFromOutside() {
        UserAccount account = UserAccount.registerCustomer(person(), EmailAddress.of("ana@gmail.com"),
                new HashedPassword("hash"));

        assertThatThrownBy(() -> account.roles().add(RoleCode.ADMIN))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void walkInPersonKeepsCounterDataAndOnlyFillsWhatIsMissing() {
        Person walkIn = Person.reconstitute(7L, DocumentNumber.of("1023456789"), PersonName.of("Ana", "Pérez"),
                PhoneNumber.ofNullable("6041234567"), null);

        walkIn.completeContactInfo(PhoneNumber.ofNullable("3001234567"), EmailAddress.of("ana@gmail.com"));

        assertThat(walkIn.phone().value()).isEqualTo("6041234567");
        assertThat(walkIn.email().value()).isEqualTo("ana@gmail.com");
    }
}
