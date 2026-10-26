package com.lavarapido.security.domain.port.in;

import com.lavarapido.security.domain.model.RoleCode;

import java.util.Set;

/**
 * Si la sesión de un token sigue abierta: no se cerró (logout, cuenta inhabilitada o desactivada) ni
 * venció. Un token firmado de una sesión cerrada ya no debe servir aunque su exp no haya llegado.
 */
public interface SessionStatusUseCase {

    boolean isActive(long userId, long sessionId);

    /**
     * E-02: además de la sesión, la cuenta sigue activa y conserva todos los roles que dice el token.
     * Si se le quitó un rol (se hace en security.user_role), se revocan todas sus sesiones.
     */
    boolean isActive(long userId, long sessionId, Set<RoleCode> tokenRoles);
}
