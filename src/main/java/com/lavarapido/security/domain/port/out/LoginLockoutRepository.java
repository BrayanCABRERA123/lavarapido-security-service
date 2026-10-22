package com.lavarapido.security.domain.port.out;

import com.lavarapido.security.domain.model.LoginLockout;

import java.util.Optional;

/** Intentos fallidos y bloqueo de cada cuenta (security.login_lockout, migración 034). */
public interface LoginLockoutRepository {

    /**
     * Bloquea los intentos de esa cuenta hasta que termine la transacción. Dos inicios de sesión
     * fallidos al mismo tiempo se cuentan uno detrás del otro: sin esto los dos leen el mismo conteo,
     * se pierde un fallo y el primer fallo de los dos choca al insertar la fila.
     */
    void lock(long userId);

    Optional<LoginLockout> findByUserId(long userId);

    void save(LoginLockout lockout);

    /** Borra los fallos y el bloqueo (inicio de sesión correcto o contraseña restablecida). */
    void clear(long userId);
}
