package com.lavarapido.security.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Primer administrador de producción (ADMIN_* en el .env del servidor). Vacío: no se crea nada.
 * La contraseña nunca se escribe en el log ni se sube al repositorio.
 */
@ConfigurationProperties(prefix = "app.bootstrap-admin")
public record BootstrapAdminProperties(String email, String password, String firstName, String lastName,
                                       String documentNumber, String phone) {

    public BootstrapAdminProperties {
        firstName = blankTo(firstName, "Administrador");
        lastName = blankTo(lastName, "Principal");
        documentNumber = blankTo(documentNumber, "1000000000");
        phone = blankTo(phone, "3000000000");
    }

    public boolean configured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }

    // la contraseña no aparece si alguien imprime la configuración
    @Override
    public String toString() {
        return "BootstrapAdminProperties[email=" + email + ", configured=" + configured() + "]";
    }
}
