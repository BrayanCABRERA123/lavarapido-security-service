package com.lavarapido.security.domain.port.in;

/**
 * Si la sesión de un token sigue abierta: no se cerró (logout, cuenta inhabilitada o desactivada) ni
 * venció. Un token firmado de una sesión cerrada ya no debe servir aunque su exp no haya llegado.
 */
public interface SessionStatusUseCase {

    boolean isActive(long userId, long sessionId);
}
