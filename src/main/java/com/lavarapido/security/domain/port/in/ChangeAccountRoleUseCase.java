package com.lavarapido.security.domain.port.in;

import com.lavarapido.security.domain.model.RoleCode;

/** Un administrador cambia el tipo de usuario (rol) de una cuenta. Nadie cambia su propio rol. */
public interface ChangeAccountRoleUseCase {

    UserAccountView changeRole(long targetUserId, RoleCode role, long actingUserId);
}
