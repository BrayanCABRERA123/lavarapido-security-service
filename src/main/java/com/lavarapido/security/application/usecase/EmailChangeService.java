package com.lavarapido.security.application.usecase;

import com.lavarapido.security.domain.event.EmailChanged;
import com.lavarapido.security.domain.exception.EmailAlreadyRegisteredException;
import com.lavarapido.security.domain.exception.IncorrectCurrentPasswordException;
import com.lavarapido.security.domain.exception.UserNotFoundException;
import com.lavarapido.security.domain.model.EmailAddress;
import com.lavarapido.security.domain.model.UserAccount;
import com.lavarapido.security.domain.port.in.ChangeEmailUseCase;
import com.lavarapido.security.domain.port.in.UserAccountView;
import com.lavarapido.security.domain.port.out.DomainEventPublisher;
import com.lavarapido.security.domain.port.out.PasswordHasher;
import com.lavarapido.security.domain.port.out.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Cambio del correo de login desde el perfil, confirmado con la contraseña actual. */
@Service
@Transactional
public class EmailChangeService implements ChangeEmailUseCase {

    private final UserAccountRepository accounts;
    private final PasswordHasher passwordHasher;
    private final DomainEventPublisher events;
    private final Clock clock;

    public EmailChangeService(UserAccountRepository accounts, PasswordHasher passwordHasher,
                              DomainEventPublisher events, Clock clock) {
        this.accounts = accounts;
        this.passwordHasher = passwordHasher;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public UserAccountView changeEmail(ChangeEmailCommand command) {
        UserAccount account = accounts.findById(command.userId()).orElseThrow(UserNotFoundException::new);
        EmailAddress newEmail = EmailAddress.of(command.newEmail());

        // la contraseña protege la cuenta si alguien encuentra la sesión abierta
        if (!passwordHasher.matches(command.currentPassword(), account.passwordHash())) {
            throw new IncorrectCurrentPasswordException();
        }

        // mismo correo (quizá con otras mayúsculas): no hay nada que cambiar
        if (newEmail.equals(account.username())) {
            return UserAccountView.from(account);
        }

        if (accounts.existsByUsername(newEmail)) {
            throw new EmailAlreadyRegisteredException();
        }

        account.changeEmail(newEmail);
        UserAccount saved = accounts.save(account);
        events.publish(new EmailChanged(saved.id(), clock.instant()));
        return UserAccountView.from(saved);
    }
}
