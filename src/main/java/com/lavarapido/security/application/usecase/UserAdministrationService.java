package com.lavarapido.security.application.usecase;

import com.lavarapido.security.domain.event.AccountDeactivated;
import com.lavarapido.security.domain.exception.CannotDisableOwnAccountException;
import com.lavarapido.security.domain.exception.UserNotFoundException;
import com.lavarapido.security.domain.model.PageResult;
import com.lavarapido.security.domain.model.UserAccount;
import com.lavarapido.security.domain.port.in.ChangeAccountStatusUseCase;
import com.lavarapido.security.domain.port.in.ListUserAccountsUseCase;
import com.lavarapido.security.domain.port.in.UserAccountView;
import com.lavarapido.security.domain.port.out.DomainEventPublisher;
import com.lavarapido.security.domain.port.out.UserAccountRepository;
import com.lavarapido.security.domain.port.out.UserSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/** Gestión de cuentas reservada a los administradores. */
@Service
@Transactional
public class UserAdministrationService implements ListUserAccountsUseCase, ChangeAccountStatusUseCase {

    static final int MAX_PAGE_SIZE = 100;

    private final UserAccountRepository accounts;
    private final UserSessionRepository sessions;
    private final DomainEventPublisher events;
    private final Clock clock;

    public UserAdministrationService(UserAccountRepository accounts, UserSessionRepository sessions,
                                     DomainEventPublisher events, Clock clock) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<UserAccountView> listAccounts(ListUserAccountsQuery query) {
        int page = Math.max(query.page(), 0);
        int size = Math.clamp(query.size(), 1, MAX_PAGE_SIZE);
        return accounts.findAll(query.role(), page, size).map(UserAccountView::from);
    }

    @Override
    public UserAccountView changeStatus(long targetUserId, boolean active, long actingUserId) {
        if (!active && targetUserId == actingUserId) {
            throw new CannotDisableOwnAccountException();
        }
        UserAccount account = accounts.findById(targetUserId).orElseThrow(UserNotFoundException::new);
        boolean wasActive = account.isActive();

        if (active) {
            account.activate();
        } else {
            account.deactivate();
        }
        UserAccountView saved = UserAccountView.from(accounts.save(account));

        // al desactivar, la persona sale de todos los equipos donde tenía sesión abierta
        if (wasActive && !active) {
            Instant now = clock.instant();
            sessions.revokeAll(targetUserId, now);
            events.publish(new AccountDeactivated(targetUserId, actingUserId,
                    AccountDeactivated.Reason.DISABLED_BY_ADMIN, now));
        }
        return saved;
    }
}
