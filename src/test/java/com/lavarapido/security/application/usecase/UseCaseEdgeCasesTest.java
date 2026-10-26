package com.lavarapido.security.application.usecase;

import com.lavarapido.security.application.fake.Fakes.InMemoryUserSessionRepository;
import com.lavarapido.security.application.fake.Fakes.MutableClock;
import com.lavarapido.security.application.fake.Fakes.PlainPasswordHasher;
import com.lavarapido.security.application.fake.Fakes.RecordingEventPublisher;
import com.lavarapido.security.application.fake.InMemoryUserAccountRepository;
import com.lavarapido.security.domain.exception.AdminAccountCannotBeDisabledException;
import com.lavarapido.security.domain.exception.CannotDisableOwnAccountException;
import com.lavarapido.security.domain.exception.InvalidValueException;
import com.lavarapido.security.domain.exception.WeakPasswordException;
import com.lavarapido.security.domain.model.DocumentNumber;
import com.lavarapido.security.domain.model.EmailAddress;
import com.lavarapido.security.domain.model.PhoneNumber;
import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.model.UserAccount;
import com.lavarapido.security.domain.service.PasswordPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas CASE / casos de uso y casos borde del dominio:
 * - Reglas de frontera en políticas de contraseña (límites de 8 a 72 caracteres, caracteres especiales).
 * - Protección de cuentas administradoras contra desactivación propia o administrativa.
 * - Validación de objetos de valor del dominio (EmailAddress, DocumentNumber, PhoneNumber).
 */
class UseCaseEdgeCasesTest {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");

    private final InMemoryUserAccountRepository accounts = new InMemoryUserAccountRepository();
    private final InMemoryUserSessionRepository sessions = new InMemoryUserSessionRepository();
    private final MutableClock clock = new MutableClock(NOW);
    private final PlainPasswordHasher hasher = new PlainPasswordHasher();
    private final PasswordPolicy passwordPolicy = new PasswordPolicy();
    private final RecordingEventPublisher events = new RecordingEventPublisher();

    private final UserAdministrationService adminService = new UserAdministrationService(accounts, sessions,
            events, clock);

    private long adminId;
    private long clientId;

    @BeforeEach
    void setupAccounts() {
        UserAccount adminAccount = UserAccount.create(
                com.lavarapido.security.domain.model.Person.create(
                        DocumentNumber.of("1000000001"),
                        com.lavarapido.security.domain.model.PersonName.of("Admin", "Master"),
                        null, EmailAddress.of("admin@gmail.com")),
                EmailAddress.of("admin@gmail.com"),
                hasher.hash("Admin2026!"),
                Set.of(RoleCode.ADMIN)
        );
        UserAccount savedAdmin = accounts.save(adminAccount);
        adminId = savedAdmin.id();

        UserAccount clientAccount = UserAccount.create(
                com.lavarapido.security.domain.model.Person.create(
                        DocumentNumber.of("1023456789"),
                        com.lavarapido.security.domain.model.PersonName.of("Juan", "Pérez"),
                        null, EmailAddress.of("juan@gmail.com")),
                EmailAddress.of("juan@gmail.com"),
                hasher.hash("Juan2026!"),
                Set.of(RoleCode.CLIENT)
        );
        UserAccount savedClient = accounts.save(clientAccount);
        clientId = savedClient.id();
    }

    @Test
    void passwordPolicyAcceptsExactBoundaryLength8And72Chars() {
        // 8 caracteres (mínimo permitido) -> 1 mayúscula, 1 minúscula, 1 número, 1 símbolo
        String pass8 = "Abc1234!";
        assertThatCode(() -> passwordPolicy.validate(pass8)).doesNotThrowAnyException();

        // 72 caracteres (máximo permitido para BCrypt)
        String pass72 = "A1!" + "a".repeat(69);
        assertThat(pass72).hasSize(72);
        assertThatCode(() -> passwordPolicy.validate(pass72)).doesNotThrowAnyException();
    }

    @Test
    void passwordPolicyRejects7CharsOr73Chars() {
        // 7 caracteres (demasiado corta)
        String pass7 = "Abc123!";
        assertThatThrownBy(() -> passwordPolicy.validate(pass7))
                .isInstanceOf(WeakPasswordException.class)
                .extracting("violations").asList().contains("MIN_LENGTH");

        // 73 caracteres (demasiado larga)
        String pass73 = "A1!" + "a".repeat(70);
        assertThat(pass73).hasSize(73);
        assertThatThrownBy(() -> passwordPolicy.validate(pass73))
                .isInstanceOf(WeakPasswordException.class)
                .extracting("violations").asList().contains("MAX_LENGTH");
    }

    @Test
    void cannotDisableAdminAccountOrSelf() {
        // Desactivarse a sí mismo
        assertThatThrownBy(() -> adminService.changeStatus(adminId, false, adminId))
                .isInstanceOf(CannotDisableOwnAccountException.class);

        // Intento de desactivar a un administrador desde otra cuenta
        assertThatThrownBy(() -> adminService.changeStatus(adminId, false, clientId))
                .isInstanceOf(AdminAccountCannotBeDisabledException.class);
    }

    @Test
    void canDisableAndEnableClientAccount() {
        adminService.changeStatus(clientId, false, adminId);
        assertThat(accounts.findById(clientId).orElseThrow().isActive()).isFalse();

        adminService.changeStatus(clientId, true, adminId);
        assertThat(accounts.findById(clientId).orElseThrow().isActive()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "@domain.com",
            "user@",
            "user@domain",
            "user space@domain.com",
            "not an email"
    })
    void emailAddressValueObjectRejectsInvalidFormats(String invalidEmail) {
        assertThatThrownBy(() -> EmailAddress.of(invalidEmail))
                .isInstanceOf(InvalidValueException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "123",                      // < 5 caracteres
            "10!234",                  // Contiene caracteres no alfanuméricos
            "123456789012345678901"    // > 20 caracteres
    })
    void documentNumberValueObjectRejectsInvalidFormats(String invalidDoc) {
        assertThatThrownBy(() -> DocumentNumber.of(invalidDoc))
                .isInstanceOf(InvalidValueException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "123456",            // < 7
            "abcdefghij",        // Letras no permitidas en teléfono
            "3001234567890123"   // > 15
    })
    void phoneNumberValueObjectRejectsInvalidFormats(String invalidPhone) {
        assertThatThrownBy(() -> PhoneNumber.ofNullable(invalidPhone))
                .isInstanceOf(InvalidValueException.class);
    }
}
