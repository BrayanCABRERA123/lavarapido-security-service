package com.lavarapido.security.infrastructure.config;

import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.port.in.BootstrapAdministratorUseCase;
import com.lavarapido.security.domain.port.in.CreateUserAccountUseCase.CreateUserAccountCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Al arrancar, si no hay ningún administrador, crea el de ADMIN_EMAIL / ADMIN_PASSWORD (producción:
 * la base empieza vacía y el registro público solo crea clientes).
 *
 * Nunca tumba el servicio: si algo falla (datos inválidos, base sin responder...) lo dice en el log
 * y security-service sigue arrancando, porque el login y todo lo demás funcionan sin esto.
 */
@Component
@EnableConfigurationProperties(BootstrapAdminProperties.class)
class AdministratorBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministratorBootstrapRunner.class);

    private final BootstrapAdministratorUseCase bootstrap;
    private final BootstrapAdminProperties properties;

    AdministratorBootstrapRunner(BootstrapAdministratorUseCase bootstrap, BootstrapAdminProperties properties) {
        this.bootstrap = bootstrap;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!properties.configured()) {
                if (!bootstrap.administratorExists()) {
                    log.warn("There is no administrator yet: set ADMIN_EMAIL and ADMIN_PASSWORD to create the first one");
                }
                return;
            }
            bootstrap.ensureAdministrator(new CreateUserAccountCommand(properties.documentNumber(),
                            properties.firstName(), properties.lastName(), properties.email(), properties.phone(),
                            properties.password(), Set.of(RoleCode.ADMIN)))
                    .ifPresentOrElse(
                            admin -> log.info("First administrator created: {} (user {})", admin.email(), admin.id()),
                            () -> log.info("An administrator already exists: ADMIN_EMAIL is not used"));
        } catch (RuntimeException e) {
            log.error("The first-administrator check failed ({}): the service keeps running; fix ADMIN_* or the "
                    + "database and restart", e.getMessage());
        }
    }
}
