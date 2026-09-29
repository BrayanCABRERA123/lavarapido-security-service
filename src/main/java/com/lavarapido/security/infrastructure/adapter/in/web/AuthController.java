package com.lavarapido.security.infrastructure.adapter.in.web;

import com.lavarapido.security.domain.port.in.AuthenticateUserUseCase;
import com.lavarapido.security.domain.port.in.AuthenticateUserUseCase.LoginCommand;
import com.lavarapido.security.domain.port.in.AuthenticationResult;
import com.lavarapido.security.domain.port.in.LogoutUseCase;
import com.lavarapido.security.domain.port.in.RegisterUserUseCase;
import com.lavarapido.security.domain.port.in.RegisterUserUseCase.RegisterUserCommand;
import com.lavarapido.security.domain.port.in.RequestPasswordResetUseCase;
import com.lavarapido.security.domain.port.in.RequestPasswordResetUseCase.RequestPasswordResetCommand;
import com.lavarapido.security.domain.port.in.ResetPasswordUseCase;
import com.lavarapido.security.domain.port.in.ResetPasswordUseCase.ResetPasswordCommand;
import com.lavarapido.security.domain.port.in.VerifyPasswordResetCodeUseCase;
import com.lavarapido.security.domain.port.in.VerifyPasswordResetCodeUseCase.VerifyResetCodeCommand;
import com.lavarapido.security.infrastructure.adapter.in.web.dto.ForgotPasswordRequest;
import com.lavarapido.security.infrastructure.adapter.in.web.dto.LoginRequest;
import com.lavarapido.security.infrastructure.adapter.in.web.dto.LoginResponse;
import com.lavarapido.security.infrastructure.adapter.in.web.dto.RegisterRequest;
import com.lavarapido.security.infrastructure.adapter.in.web.dto.ResetPasswordRequest;
import com.lavarapido.security.infrastructure.adapter.in.web.dto.UserResponse;
import com.lavarapido.security.infrastructure.adapter.in.web.dto.VerifyResetCodeRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

/** Endpoints públicos de identidad: registro, login, logout y recuperación de contraseña. */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private final RegisterUserUseCase registerUser;
    private final AuthenticateUserUseCase authenticateUser;
    private final LogoutUseCase logout;
    private final RequestPasswordResetUseCase requestPasswordReset;
    private final VerifyPasswordResetCodeUseCase verifyPasswordResetCode;
    private final ResetPasswordUseCase resetPassword;

    AuthController(RegisterUserUseCase registerUser, AuthenticateUserUseCase authenticateUser, LogoutUseCase logout,
                   RequestPasswordResetUseCase requestPasswordReset,
                   VerifyPasswordResetCodeUseCase verifyPasswordResetCode, ResetPasswordUseCase resetPassword) {
        this.registerUser = registerUser;
        this.authenticateUser = authenticateUser;
        this.logout = logout;
        this.requestPasswordReset = requestPasswordReset;
        this.verifyPasswordResetCode = verifyPasswordResetCode;
        this.resetPassword = resetPassword;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(registerUser.register(new RegisterUserCommand(request.documentNumber(),
                request.firstName(), request.lastName(), request.email(), request.phone(), request.password())));
    }

    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        AuthenticationResult result = authenticateUser.login(new LoginCommand(request.email(), request.password(),
                http.getRemoteAddr(), http.getHeader(HttpHeaders.USER_AGENT)));

        long expiresIn = Math.max(0, Duration.between(Instant.now(), result.expiresAt()).toSeconds());
        LoginResponse body = new LoginResponse(result.accessToken(), "Bearer", expiresIn, result.expiresAt(),
                UserResponse.from(result.user()));
        // Una respuesta con token nunca debe quedar en caché del navegador ni de un proxy (RFC 6749, 5.1).
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(body);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUser caller = AuthenticatedUser.from(jwt);
        logout.logout(caller.userId(), caller.sessionId());
    }

    /** Siempre 202, exista o no el correo: la respuesta no debe revelar cuentas. */
    @PostMapping("/password/forgot")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest http) {
        requestPasswordReset.requestReset(new RequestPasswordResetCommand(request.email(), http.getRemoteAddr()));
    }

    @PostMapping("/password/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {
        verifyPasswordResetCode.verifyCode(new VerifyResetCodeCommand(request.email(), request.code()));
    }

    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        resetPassword.resetPassword(new ResetPasswordCommand(request.email(), request.code(), request.newPassword()));
    }
}
