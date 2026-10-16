package com.lavarapido.security.infrastructure.config;

import com.lavarapido.security.domain.exception.DomainException;
import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.port.in.BootstrapAdministratorUseCase;
import com.lavarapido.security.domain.port.in.CreateUserAccountUseCase.CreateUserAccountCommand;
import com.lavarapido.security.domain.port.out.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Al arrancar, si no hay ningún administrador, crea el de ADMIN_EMAIL / ADMIN_PASSWORD (producción:
 * la base empieza vacía y el registro público solo crea clientes). Si los datos no sirven (ej.
 * contraseña débil) lo dice en el log y el servicio sigue arrancando: el resto funciona igual.
 */
@Component
@EnableConfigurationProperties(BootstrapAdminProperties.class)
class AdministratorBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministratorBootstrapRunner.class);

    private final BootstrapAdministratorUseCase bootstrap;
    private final UserAccountRepository accounts;
    private final BootstrapAdminProperties properties;

    AdministratorBootstrapRunner(BootstrapAdministratorUseCase bootstrap, UserAccountRepository accounts,
                                 BootstrapAdminProperties properties) {
        this.bootstrap = bootstrap;
        this.accounts = accounts;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.configured()) {
            if (accounts.findAll(RoleCode.ADMIN, 0, 1).totalElements() == 0) {
                log.warn("There is no administrator yet: set ADMIN_EMAIL and ADMIN_PASSWORD to create the first one");
            }
            return;
        }
        try {
            bootstrap.ensureAdministrator(new CreateUserAccountCommand(properties.documentNumber(),
                            properties.firstName(), properties.lastName(), properties.email(), properties.phone(),
                            properties.password(), Set.of(RoleCode.ADMIN)))
                    .ifPresentOrElse(
                            admin -> log.info("First administrator created: {} (user {})", admin.email(), admin.id()),
                            () -> log.info("An administrator already exists: ADMIN_EMAIL is not used"));
        } catch (DomainException e) {
            log.error("The first administrator could not be created ({}): fix ADMIN_* and restart", e.getMessage());
        }
    }
}
