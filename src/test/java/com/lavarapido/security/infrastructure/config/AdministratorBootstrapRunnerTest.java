package com.lavarapido.security.infrastructure.config;

import com.lavarapido.security.domain.port.in.BootstrapAdministratorUseCase;
import com.lavarapido.security.domain.port.in.CreateUserAccountUseCase.CreateUserAccountCommand;
import com.lavarapido.security.domain.port.in.UserAccountView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** El arranque del primer administrador nunca debe impedir que security-service arranque. */
class AdministratorBootstrapRunnerTest {

    private static final BootstrapAdminProperties NOT_CONFIGURED =
            new BootstrapAdminProperties("", "", null, null, null, null);
    private static final BootstrapAdminProperties CONFIGURED =
            new BootstrapAdminProperties("dueno@gmail.com", "Lavado2026!", null, null, null, null);

    /** Caso de uso que falla como la base cuando la consulta salía de la transacción. */
    private static final class BrokenBootstrap implements BootstrapAdministratorUseCase {
        @Override
        public boolean administratorExists() {
            throw new IllegalStateException("Cannot lazily initialize collection (no session)");
        }

        @Override
        public Optional<UserAccountView> ensureAdministrator(CreateUserAccountCommand administrator) {
            throw new IllegalStateException("database down");
        }
    }

    private static final class RecordingBootstrap implements BootstrapAdministratorUseCase {
        final List<String> calls = new ArrayList<>();

        @Override
        public boolean administratorExists() {
            calls.add("exists");
            return true;
        }

        @Override
        public Optional<UserAccountView> ensureAdministrator(CreateUserAccountCommand administrator) {
            calls.add("ensure:" + administrator.email());
            return Optional.empty();
        }
    }

    @Test
    void withoutAdminVariablesAFailingCheckDoesNotStopTheService() {
        AdministratorBootstrapRunner runner = new AdministratorBootstrapRunner(new BrokenBootstrap(), NOT_CONFIGURED);

        assertThatCode(() -> runner.run(new DefaultApplicationArguments())).doesNotThrowAnyException();
    }

    @Test
    void withAdminVariablesAFailingCreationDoesNotStopTheService() {
        AdministratorBootstrapRunner runner = new AdministratorBootstrapRunner(new BrokenBootstrap(), CONFIGURED);

        assertThatCode(() -> runner.run(new DefaultApplicationArguments())).doesNotThrowAnyException();
    }

    @Test
    void withoutAdminVariablesItOnlyChecksAndCreatesNothing() {
        RecordingBootstrap bootstrap = new RecordingBootstrap();

        new AdministratorBootstrapRunner(bootstrap, NOT_CONFIGURED).run(new DefaultApplicationArguments());

        assertThat(bootstrap.calls).containsExactly("exists");
    }

    @Test
    void withAdminVariablesItAsksToEnsureTheAdministrator() {
        RecordingBootstrap bootstrap = new RecordingBootstrap();

        new AdministratorBootstrapRunner(bootstrap, CONFIGURED).run(new DefaultApplicationArguments());

        assertThat(bootstrap.calls).containsExactly("ensure:dueno@gmail.com");
    }
}
