package com.lavarapido.security.application.usecase;

import com.lavarapido.security.domain.event.AccountDeactivated;
import com.lavarapido.security.domain.exception.IncorrectCurrentPasswordException;
import com.lavarapido.security.domain.exception.UserNotFoundException;
import com.lavarapido.security.domain.model.UserAccount;
import com.lavarapido.security.domain.port.in.DeactivateOwnAccountUseCase;
import com.lavarapido.security.domain.port.out.DomainEventPublisher;
import com.lavarapido.security.domain.port.out.PasswordHasher;
import com.lavarapido.security.domain.port.out.UserAccountRepository;
import com.lavarapido.security.domain.port.out.UserSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/** El usuario cierra su cuenta desde el perfil: se desactiva, no se borra. */
@Service
@Transactional
public class AccountDeactivationService implements DeactivateOwnAccountUseCase {

    private final UserAccountRepository accounts;
    private final UserSessionRepository sessions;
    private final PasswordHasher passwordHasher;
    private final DomainEventPublisher events;
    private final Clock clock;

    public AccountDeactivationService(UserAccountRepository accounts, UserSessionRepository sessions,
                                      PasswordHasher passwordHasher, DomainEventPublisher events, Clock clock) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.passwordHasher = passwordHasher;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void deactivateOwnAccount(DeactivateOwnAccountCommand command) {
        UserAccount account = accounts.findById(command.userId()).orElseThrow(UserNotFoundException::new);

        // se pide la contraseña para que nadie cierre la cuenta desde una sesión que quedó abierta
        if (!passwordHasher.matches(command.currentPassword(), account.passwordHash())) {
            throw new IncorrectCurrentPasswordException();
        }

        Instant now = clock.instant();
        account.deactivate();
        accounts.save(account);
        // sale de todos los equipos donde tenía sesión abierta
        sessions.revokeAll(account.id(), now);
        events.publish(new AccountDeactivated(account.id(), account.id(),
                AccountDeactivated.Reason.CLOSED_BY_USER, now));
    }
}
