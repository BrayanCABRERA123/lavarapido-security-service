package com.lavarapido.security.domain.port.in;

import com.lavarapido.security.domain.model.RoleCode;

import java.util.Set;

/** Un administrador crea una cuenta con roles explícitos (ej. un operario nuevo). */
public interface CreateUserAccountUseCase {

    record CreateUserAccountCommand(
            String documentNumber,
            String firstName,
            String lastName,
            String email,
            String phone,
            String password,
            Set<RoleCode> roles) {
    }

    UserAccountView createAccount(CreateUserAccountCommand command);
}
