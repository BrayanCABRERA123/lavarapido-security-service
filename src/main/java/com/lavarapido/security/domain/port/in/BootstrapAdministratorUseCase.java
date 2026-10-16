package com.lavarapido.security.domain.port.in;

import com.lavarapido.security.domain.port.in.CreateUserAccountUseCase.CreateUserAccountCommand;

import java.util.Optional;

/**
 * Primer administrador de una instalación nueva (producción): sin él nadie puede entrar al panel,
 * porque el registro público solo crea clientes. Solo actúa si todavía no hay ningún administrador.
 */
public interface BootstrapAdministratorUseCase {

    /** Si ya existe al menos un administrador (activo o no). */
    boolean administratorExists();

    /**
     * Crea la cuenta como ADMIN si no existe ningún administrador (se ignoran los roles del comando).
     *
     * @return la cuenta creada, o vacío si ya había un administrador
     */
    Optional<UserAccountView> ensureAdministrator(CreateUserAccountCommand administrator);
}
