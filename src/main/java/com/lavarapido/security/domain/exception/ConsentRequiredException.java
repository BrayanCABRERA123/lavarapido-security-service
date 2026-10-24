package com.lavarapido.security.domain.exception;

/**
 * Para abrir una cuenta hay que aceptar los Términos y la Política de Tratamiento de Datos
 * (Ley 1581 de 2012, RF-001). Sin las dos casillas no se registra nada.
 */
public class ConsentRequiredException extends DomainException {

    public ConsentRequiredException() {
        super("CONSENT_REQUIRED", "The Terms and the Data Processing Policy must be accepted to register");
    }
}
