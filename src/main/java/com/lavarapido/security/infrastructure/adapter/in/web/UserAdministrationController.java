package com.lavarapido.security.infrastructure.adapter.in.web;

import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.port.in.ChangeAccountStatusUseCase;
import com.lavarapido.security.domain.port.in.CreateUserAccountUseCase;
import com.lavarapido.security.domain.port.in.CreateUserAccountUseCase.CreateUserAccountCommand;
import com.lavarapido.security.domain.port.in.ListUserAccountsUseCase;
import com.lavarapido.security.domain.port.in.ListUserAccountsUseCase.ListUserAccountsQuery;
import com.lavarapido.security.infrastructure.adapter.in.web.dto.CreateUserRequest;
import com.lavarapido.security.infrastructure.adapter.in.web.dto.PageResponse;
import com.lavarapido.security.infrastructure.adapter.in.web.dto.UpdateStatusRequest;
import com.lavarapido.security.infrastructure.adapter.in.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Gestión de cuentas. {@code /api/v1/admin/**} exige el rol ADMIN (SecurityConfig). */
@Tag(name = "Admin - users", description = "Gestión de cuentas (solo ADMIN)")
@RestController
@RequestMapping("/api/v1/admin/users")
class UserAdministrationController {

    private final CreateUserAccountUseCase createAccount;
    private final ListUserAccountsUseCase listAccounts;
    private final ChangeAccountStatusUseCase changeStatus;

    UserAdministrationController(CreateUserAccountUseCase createAccount, ListUserAccountsUseCase listAccounts,
                                 ChangeAccountStatusUseCase changeStatus) {
        this.createAccount = createAccount;
        this.listAccounts = listAccounts;
        this.changeStatus = changeStatus;
    }

    @GetMapping
    PageResponse<UserResponse> list(@RequestParam(required = false) RoleCode role,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(listAccounts.listAccounts(new ListUserAccountsQuery(role, page, size)),
                UserResponse::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return UserResponse.from(createAccount.createAccount(new CreateUserAccountCommand(request.documentNumber(),
                request.firstName(), request.lastName(), request.email(), request.phone(), request.password(),
                request.roles())));
    }

    @PatchMapping("/{userId}/status")
    UserResponse changeStatus(@PathVariable long userId, @Valid @RequestBody UpdateStatusRequest request,
                              @AuthenticationPrincipal Jwt jwt) {
        return UserResponse.from(changeStatus.changeStatus(userId, request.active(),
                AuthenticatedUser.from(jwt).userId()));
    }
}
