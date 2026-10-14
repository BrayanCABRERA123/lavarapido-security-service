package com.lavarapido.security.domain.exception;

import java.time.Instant;

/** Demasiados intentos fallidos: la cuenta está bloqueada un rato (RF-004b). */
public class AccountLockedException extends DomainException {

    private final Instant lockedUntil;

    public AccountLockedException(Instant lockedUntil) {
        super("ACCOUNT_LOCKED", "Too many failed attempts: the account is locked for a while");
        this.lockedUntil = lockedUntil;
    }

    public Instant lockedUntil() {
        return lockedUntil;
    }
}
