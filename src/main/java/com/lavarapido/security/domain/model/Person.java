package com.lavarapido.security.domain.model;

import java.util.Objects;

/**
 * Una persona que el sistema conoce. Puede existir sin cuenta (un cliente registrado en el
 * mostrador), por eso está separada de {@link UserAccount}.
 */
public final class Person {

    private final Long id;
    private final DocumentNumber documentNumber;
    private PersonName name;
    private PhoneNumber phone;
    private EmailAddress email;

    private Person(Long id, DocumentNumber documentNumber, PersonName name, PhoneNumber phone, EmailAddress email) {
        this.id = id;
        this.documentNumber = Objects.requireNonNull(documentNumber, "documentNumber");
        this.name = Objects.requireNonNull(name, "name");
        this.phone = phone;
        this.email = email;
    }

    public static Person create(DocumentNumber documentNumber, PersonName name, PhoneNumber phone, EmailAddress email) {
        return new Person(null, documentNumber, name, phone, email);
    }

    public static Person reconstitute(Long id, DocumentNumber documentNumber, PersonName name,
                                      PhoneNumber phone, EmailAddress email) {
        return new Person(Objects.requireNonNull(id, "id"), documentNumber, name, phone, email);
    }

    public void rename(PersonName newName) {
        this.name = Objects.requireNonNull(newName, "name");
    }

    public void changeEmail(EmailAddress newEmail) {
        this.email = Objects.requireNonNull(newEmail, "email");
    }

    public void changePhone(PhoneNumber newPhone) {
        this.phone = newPhone;
    }

    /**
     * Completa los datos de contacto que faltan sin sobrescribir lo que ya registró el
     * mostrador. Se usa cuando un cliente del mostrador luego crea su cuenta en línea.
     */
    public void completeContactInfo(PhoneNumber phone, EmailAddress email) {
        if (this.phone == null) {
            this.phone = phone;
        }
        if (this.email == null) {
            this.email = email;
        }
    }

    public Long id() {
        return id;
    }

    public DocumentNumber documentNumber() {
        return documentNumber;
    }

    public PersonName name() {
        return name;
    }

    public PhoneNumber phone() {
        return phone;
    }

    public EmailAddress email() {
        return email;
    }
}
