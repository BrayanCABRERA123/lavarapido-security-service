package com.lavarapido.security.domain.port.out;

import com.lavarapido.security.domain.model.LoginLockout;

import java.util.Optional;

/** Intentos fallidos y bloqueo de cada cuenta (security.login_lockout, migración 034). */
public interface LoginLockoutRepository {

    Optional<LoginLockout> findByUserId(long userId);

    void save(LoginLockout lockout);

    /** Borra los fallos y el bloqueo (inicio de sesión correcto o contraseña restablecida). */
    void clear(long userId);
}
