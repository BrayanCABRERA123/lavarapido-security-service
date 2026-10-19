package com.lavarapido.security.application.usecase;

import com.lavarapido.security.domain.event.AccountDeactivated;
import com.lavarapido.security.domain.exception.AdminAccountCannotBeDisabledException;
import com.lavarapido.security.domain.exception.CannotChangeOwnRoleException;
import com.lavarapido.security.domain.exception.CannotDisableOwnAccountException;
import com.lavarapido.security.domain.exception.UserNotFoundException;
import com.lavarapido.security.domain.model.PageResult;
import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.model.UserAccount;
import com.lavarapido.security.domain.port.in.ChangeAccountRoleUseCase;
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
public class UserAdministrationService implements ListUserAccountsUseCase, ChangeAccountStatusUseCase,
        ChangeAccountRoleUseCase {

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
        // un administrador no se desactiva: primero se le cambia el rol (el lavadero no queda sin gestión)
        if (!active && account.hasRole(RoleCode.ADMIN)) {
            throw new AdminAccountCannotBeDisabledException();
        }
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

    @Override
    public UserAccountView changeRole(long targetUserId, RoleCode role, long actingUserId) {
        if (targetUserId == actingUserId) {
            throw new CannotChangeOwnRoleException();
        }
        UserAccount account = accounts.findById(targetUserId).orElseThrow(UserNotFoundException::new);
        if (account.hasRole(role) && account.roles().size() == 1) {
            return UserAccountView.from(account);
        }
        account.changeRole(role);
        UserAccountView saved = UserAccountView.from(accounts.save(account));
        // sus tokens llevan el rol anterior: se cierran sus sesiones y entra de nuevo con el rol nuevo
        sessions.revokeAll(targetUserId, clock.instant());
        return saved;
    }
}
