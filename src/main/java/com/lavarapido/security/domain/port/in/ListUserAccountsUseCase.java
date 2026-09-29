package com.lavarapido.security.domain.port.in;

import com.lavarapido.security.domain.model.PageResult;
import com.lavarapido.security.domain.model.RoleCode;

public interface ListUserAccountsUseCase {

    /** @param role filtro opcional; {@code null} lista todas las cuentas */
    record ListUserAccountsQuery(RoleCode role, int page, int size) {
    }

    PageResult<UserAccountView> listAccounts(ListUserAccountsQuery query);
}
