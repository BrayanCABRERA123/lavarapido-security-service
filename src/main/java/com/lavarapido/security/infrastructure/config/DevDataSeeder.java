package com.lavarapido.security.infrastructure.config;

import com.lavarapido.security.domain.model.EmailAddress;
import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.port.in.CreateUserAccountUseCase;
import com.lavarapido.security.domain.port.in.CreateUserAccountUseCase.CreateUserAccountCommand;
import com.lavarapido.security.domain.port.out.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * SOLO DESARROLLO: una cuenta demo por rol para poder abrir cada área del frontend
 * apenas arranca por primera vez. Pasa por el caso de uso real, así los datos demo cumplen las mismas
 * reglas que los reales. Omite las cuentas que ya existen.
 */
@Component
@Profile("dev")
class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private static final List<CreateUserAccountCommand> DEMO_ACCOUNTS = List.of(
            new CreateUserAccountCommand("1000000001", "Admin", "Demo", "admin@gmail.com", "3000000001",
                    "Admin123!", Set.of(RoleCode.ADMIN)),
            new CreateUserAccountCommand("1000000002", "Operario", "Demo", "operador@gmail.com", "3000000002",
                    "Operador123!", Set.of(RoleCode.OPERATOR)),
            new CreateUserAccountCommand("1000000003", "Cliente", "Demo", "cliente@gmail.com", "3000000003",
                    "Cliente123!", Set.of(RoleCode.CLIENT)));

    private final CreateUserAccountUseCase createAccount;
    private final UserAccountRepository accounts;

    DevDataSeeder(CreateUserAccountUseCase createAccount, UserAccountRepository accounts) {
        this.createAccount = createAccount;
        this.accounts = accounts;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (CreateUserAccountCommand demo : DEMO_ACCOUNTS) {
            if (!accounts.existsByUsername(EmailAddress.of(demo.email()))) {
                createAccount.createAccount(demo);
                log.info("[DEV] Demo account created: {} {}", demo.email(), demo.roles());
            }
        }
    }
}
