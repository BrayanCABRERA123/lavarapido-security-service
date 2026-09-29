package com.lavarapido.security.domain.exception;

/** La cuenta existe pero un administrador la desactivó. */
public class AccountDisabledException extends DomainException {

    public AccountDisabledException() {
        super("ACCOUNT_DISABLED", "This account is disabled");
    }
}
