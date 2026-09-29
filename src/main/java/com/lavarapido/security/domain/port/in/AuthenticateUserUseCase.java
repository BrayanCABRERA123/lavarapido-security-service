package com.lavarapido.security.domain.port.in;

/** Inicio de sesión con correo y contraseña (RF-002). */
public interface AuthenticateUserUseCase {

    record LoginCommand(String email, String password, String ipAddress, String userAgent) {

        @Override
        public String toString() {
            return "LoginCommand[email=" + email + ", password=****]";
        }
    }

    AuthenticationResult login(LoginCommand command);
}
