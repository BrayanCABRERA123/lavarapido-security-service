package com.lavarapido.security.infrastructure.config;

import com.lavarapido.security.domain.port.in.CreateUserAccountUseCase;
import com.lavarapido.security.domain.port.in.CreateUserAccountUseCase.CreateUserAccountCommand;
import com.lavarapido.security.domain.port.out.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DevDataSeederTest {

    private final CreateUserAccountUseCase createAccount = mock(CreateUserAccountUseCase.class);
    private final UserAccountRepository accounts = mock(UserAccountRepository.class);

    @Test
    void withoutPasswordsInTheEnvironmentNoDemoAccountIsCreated() {
        // E-05 (H-07): las contraseñas demo ya no están en el código
        new DevDataSeeder(createAccount, accounts, "", null, " ").run(null);

        verify(createAccount, never()).createAccount(any());
    }

    @Test
    void eachDemoAccountUsesThePasswordFromItsVariable() {
        new DevDataSeeder(createAccount, accounts, "Admin-Local1!", "", "Cliente-Local1!").run(null);

        ArgumentCaptor<CreateUserAccountCommand> created = ArgumentCaptor.forClass(CreateUserAccountCommand.class);
        verify(createAccount, times(2)).createAccount(created.capture());
        assertThat(created.getAllValues()).extracting(CreateUserAccountCommand::email, CreateUserAccountCommand::password)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("admin@gmail.com", "Admin-Local1!"),
                        org.assertj.core.groups.Tuple.tuple("cliente@gmail.com", "Cliente-Local1!"));
    }
}
