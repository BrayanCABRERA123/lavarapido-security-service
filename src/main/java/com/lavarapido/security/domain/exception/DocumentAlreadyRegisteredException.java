package com.lavarapido.security.domain.exception;

/** La persona con este número de documento ya tiene cuenta (una cuenta por persona). */
public class DocumentAlreadyRegisteredException extends DomainException {

    public DocumentAlreadyRegisteredException() {
        super("DOCUMENT_ALREADY_REGISTERED", "An account for this document number already exists");
    }
}
