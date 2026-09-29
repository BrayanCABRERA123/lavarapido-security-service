package com.lavarapido.security.domain.port.in;

/** Cierra la sesión a la que pertenece el token (deja el rastro, llena {@code revoked_at}). */
public interface LogoutUseCase {

    void logout(long userId, long sessionId);
}
