package com.lavarapido.security.infrastructure.config;

import com.lavarapido.security.domain.model.EmailAddress;
import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.port.in.CreateUserAccountUseCase;
import com.lavarapido.security.domain.port.in.CreateUserAccountUseCase.CreateUserAccountCommand;
import com.lavarapido.security.domain.port.out.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SOLO DESARROLLO: una cuenta demo por rol para poder abrir cada área del frontend
 * apenas arranca por primera vez. Pasa por el caso de uso real, así los datos demo cumplen las mismas
 * reglas que los reales. Omite las cuentas que ya existen.
 *
 * E-05 (H-07): las contraseñas no están en el código. Salen de DEMO_ADMIN_PASSWORD,
 * DEMO_OPERATOR_PASSWORD y DEMO_CLIENT_PASSWORD (lavarapido-infra/.env); la cuenta cuya variable
 * esté vacía no se crea.
 */
@Component
@Profile("dev")
class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final CreateUserAccountUseCase createAccount;
    private final UserAccountRepository accounts;
    private final List<CreateUserAccountCommand> demoAccounts = new ArrayList<>();

    DevDataSeeder(CreateUserAccountUseCase createAccount, UserAccountRepository accounts,
                  @Value("${app.demo.admin-password:}") String adminPassword,
                  @Value("${app.demo.operator-password:}") String operatorPassword,
                  @Value("${app.demo.client-password:}") String clientPassword) {
        this.createAccount = createAccount;
        this.accounts = accounts;
        addIfConfigured("1000000001", "Admin", "admin@gmail.com", "3000000001", adminPassword, RoleCode.ADMIN,
                "DEMO_ADMIN_PASSWORD");
        addIfConfigured("1000000002", "Operario", "operador@gmail.com", "3000000002", operatorPassword,
                RoleCode.OPERATOR, "DEMO_OPERATOR_PASSWORD");
        addIfConfigured("1000000003", "Cliente", "cliente@gmail.com", "3000000003", clientPassword, RoleCode.CLIENT,
                "DEMO_CLIENT_PASSWORD");
    }

    private void addIfConfigured(String document, String firstName, String email, String phone, String password,
                                 RoleCode role, String variable) {
        if (password == null || password.isBlank()) {
            log.info("[DEV] {} is empty: the demo account {} is not created", variable, email);
            return;
        }
        demoAccounts.add(new CreateUserAccountCommand(document, firstName, "Demo", email, phone, password, Set.of(role)));
    }

    @Override
    public void run(ApplicationArguments args) {
        for (CreateUserAccountCommand demo : demoAccounts) {
            if (!accounts.existsByUsername(EmailAddress.of(demo.email()))) {
                createAccount.createAccount(demo);
                log.info("[DEV] Demo account created: {} {}", demo.email(), demo.roles());
            }
        }
    }
}
