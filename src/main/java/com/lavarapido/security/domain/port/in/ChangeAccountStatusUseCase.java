package com.lavarapido.security.domain.port.in;

/** Un administrador activa o desactiva una cuenta. Nadie puede desactivar su propia cuenta. */
public interface ChangeAccountStatusUseCase {

    UserAccountView changeStatus(long targetUserId, boolean active, long actingUserId);
}
