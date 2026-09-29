package com.lavarapido.security.application.usecase;

import com.lavarapido.security.domain.exception.CannotDisableOwnAccountException;
import com.lavarapido.security.domain.exception.UserNotFoundException;
import com.lavarapido.security.domain.model.PageResult;
import com.lavarapido.security.domain.model.UserAccount;
import com.lavarapido.security.domain.port.in.ChangeAccountStatusUseCase;
import com.lavarapido.security.domain.port.in.ListUserAccountsUseCase;
import com.lavarapido.security.domain.port.in.UserAccountView;
import com.lavarapido.security.domain.port.out.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestión de cuentas reservada a los administradores. */
@Service
@Transactional
public class UserAdministrationService implements ListUserAccountsUseCase, ChangeAccountStatusUseCase {

    static final int MAX_PAGE_SIZE = 100;

    private final UserAccountRepository accounts;

    public UserAdministrationService(UserAccountRepository accounts) {
        this.accounts = accounts;
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
        if (active) {
            account.activate();
        } else {
            account.deactivate();
        }
        return UserAccountView.from(accounts.save(account));
    }
}
