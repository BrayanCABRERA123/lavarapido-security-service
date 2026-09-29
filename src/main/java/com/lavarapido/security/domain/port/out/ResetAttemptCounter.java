package com.lavarapido.security.domain.port.out;

/** Cuenta los intentos fallidos por código, para que un código de 6 dígitos no se pueda adivinar por fuerza bruta. */
public interface ResetAttemptCounter {

    /** @return la cantidad de fallos registrados para este código, incluido este */
    int recordFailure(long tokenId);

    void clear(long tokenId);
}
