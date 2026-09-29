package com.lavarapido.security.domain.port.out;

import com.lavarapido.security.domain.model.HashedPassword;

/** Hash de una sola vía para contraseñas y códigos de recuperación (BCrypt en producción). */
public interface PasswordHasher {

    HashedPassword hash(String raw);

    boolean matches(String raw, HashedPassword hash);
}
