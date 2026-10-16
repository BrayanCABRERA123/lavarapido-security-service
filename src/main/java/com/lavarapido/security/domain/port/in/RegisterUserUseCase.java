package com.lavarapido.security.domain.port.in;

/** Autorregistro de un cliente (RF-001). */
public interface RegisterUserUseCase {

    record RegisterUserCommand(
            String documentNumber,
            String firstName,
            String lastName,
            String email,
            String phone,
            String password,
            // marcó las casillas de los Términos y de la Política de Tratamiento de Datos
            boolean acceptsTerms,
            boolean acceptsDataPolicy) {

        /** Sin las casillas (clientes que todavía no las envían): no queda constancia. */
        public RegisterUserCommand(String documentNumber, String firstName, String lastName, String email,
                                   String phone, String password) {
            this(documentNumber, firstName, lastName, email, phone, password, false, false);
        }
    }

    UserAccountView register(RegisterUserCommand command);
}
