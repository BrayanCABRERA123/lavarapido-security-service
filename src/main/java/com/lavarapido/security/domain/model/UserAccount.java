package com.lavarapido.security.domain.model;

import com.lavarapido.security.domain.exception.AccountDisabledException;
import com.lavarapido.security.domain.exception.InvalidValueException;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Raíz del agregado del contexto de identidad: las credenciales de acceso de una {@link Person}.
 *
 * <p>Invariantes:
 * <ul>
 *   <li>Una cuenta por persona (lo valida el caso de uso, respaldado por {@code uq_app_user_person}).</li>
 *   <li>El usuario es el correo. Solo cambia con {@link #changeEmail}, que actualiza también el de la persona.</li>
 *   <li>Una cuenta siempre tiene al menos un rol.</li>
 *   <li>Una cuenta desactivada no puede autenticarse.</li>
 * </ul>
 */
public final class UserAccount {

    /** {@code app_user.username} es NVARCHAR(60), más corto que un correo de contacto. */
    static final int MAX_USERNAME_LENGTH = 60;

    private final Long id;
    private final Person person;
    private EmailAddress username;
    private HashedPassword passwordHash;
    private boolean active;
    private Instant lastLogin;
    private final Set<RoleCode> roles;

    private UserAccount(Long id, Person person, EmailAddress username, HashedPassword passwordHash,
                        boolean active, Instant lastLogin, Set<RoleCode> roles) {
        this.id = id;
        this.person = Objects.requireNonNull(person, "person");
        this.username = requireLoginEmail(username);
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.active = active;
        this.lastLogin = lastLogin;
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("An account must hold at least one role");
        }
        this.roles = EnumSet.copyOf(roles);
    }

    /** Autorregistro: toda cuenta nueva en línea empieza como cliente. */
    public static UserAccount registerCustomer(Person person, EmailAddress email, HashedPassword passwordHash) {
        return create(person, email, passwordHash, Set.of(RoleCode.CLIENT));
    }

    /** Cuenta creada por un administrador (ej. un operario), con roles explícitos. */
    public static UserAccount create(Person person, EmailAddress email, HashedPassword passwordHash, Set<RoleCode> roles) {
        return new UserAccount(null, person, email, passwordHash, true, null, roles);
    }

    public static UserAccount reconstitute(Long id, Person person, EmailAddress username, HashedPassword passwordHash,
                                           boolean active, Instant lastLogin, Set<RoleCode> roles) {
        return new UserAccount(Objects.requireNonNull(id, "id"), person, username, passwordHash, active, lastLogin, roles);
    }

    public void ensureCanAuthenticate() {
        if (!active) {
            throw new AccountDisabledException();
        }
    }

    public void recordSuccessfulLogin(Instant at) {
        ensureCanAuthenticate();
        this.lastLogin = Objects.requireNonNull(at, "at");
    }

    /**
     * Cambia el correo con el que se inicia sesión. El caso de uso verifica antes la contraseña
     * y que el correo no esté tomado por otra cuenta.
     */
    public void changeEmail(EmailAddress newEmail) {
        this.username = requireLoginEmail(newEmail);
        person.changeEmail(newEmail);
    }

    private static EmailAddress requireLoginEmail(EmailAddress email) {
        Objects.requireNonNull(email, "username");
        if (email.value().length() > MAX_USERNAME_LENGTH) {
            throw new InvalidValueException("INVALID_EMAIL", "Email is too long to be used as a login");
        }
        return email;
    }

    public void changePassword(HashedPassword newHash) {
        this.passwordHash = Objects.requireNonNull(newHash, "newHash");
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean hasRole(RoleCode role) {
        return roles.contains(role);
    }

    public Long id() {
        return id;
    }

    public Person person() {
        return person;
    }

    public EmailAddress username() {
        return username;
    }

    public HashedPassword passwordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return active;
    }

    public Instant lastLogin() {
        return lastLogin;
    }

    public Set<RoleCode> roles() {
        return Collections.unmodifiableSet(roles);
    }
}
