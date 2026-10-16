package com.lavarapido.security.application.usecase;

import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.port.in.BootstrapAdministratorUseCase;
import com.lavarapido.security.domain.port.in.CreateUserAccountUseCase;
import com.lavarapido.security.domain.port.in.CreateUserAccountUseCase.CreateUserAccountCommand;
import com.lavarapido.security.domain.port.in.UserAccountView;
import com.lavarapido.security.domain.port.out.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

/**
 * Crea el primer administrador por el mismo caso de uso que usa un administrador para crear
 * cuentas: la contraseña cumple la política, el documento y el correo no se repiten y se publica
 * UserRegistered (bienvenida). Si ya existe algún administrador, no hace nada.
 */
@Service
public class AdministratorBootstrapService implements BootstrapAdministratorUseCase {

    private final UserAccountRepository accounts;
    private final CreateUserAccountUseCase createAccount;

    public AdministratorBootstrapService(UserAccountRepository accounts, CreateUserAccountUseCase createAccount) {
        this.accounts = accounts;
        this.createAccount = createAccount;
    }

    @Override
    @Transactional
    public Optional<UserAccountView> ensureAdministrator(CreateUserAccountCommand administrator) {
        if (accounts.findAll(RoleCode.ADMIN, 0, 1).totalElements() > 0) {
            return Optional.empty();
        }
        CreateUserAccountCommand asAdmin = new CreateUserAccountCommand(administrator.documentNumber(),
                administrator.firstName(), administrator.lastName(), administrator.email(), administrator.phone(),
                administrator.password(), Set.of(RoleCode.ADMIN));
        return Optional.of(createAccount.createAccount(asAdmin));
    }
}
