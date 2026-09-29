package com.lavarapido.security.domain.model;

import java.util.Objects;

/**
 * Hash de una contraseña. El dominio nunca guarda la contraseña en texto plano más allá del caso
 * de uso que la recibe, y este tipo nunca imprime su valor.
 */
public record HashedPassword(String value) {

    public HashedPassword {
        Objects.requireNonNull(value, "hash");
        if (value.isBlank()) {
            throw new IllegalArgumentException("hash must not be blank");
        }
    }

    @Override
    public String toString() {
        return "HashedPassword[****]";
    }
}
