package com.lavarapido.security.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI del servicio. Swagger UI queda en {@code /swagger-ui.html} y solo se
 * activa en el perfil dev (ver application-dev.yml): en producción no se publica el mapa de la API.
 */
@Configuration
class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    OpenAPI securityServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("LavaRapido — security-service")
                        .version("v1")
                        .description("""
                                Cuentas, login con JWT, recuperación de contraseña, perfil, preferencias \
                                y administración de usuarios. Para probar los endpoints protegidos: haz \
                                login en POST /api/v1/auth/login, copia el accessToken y pégalo en el \
                                botón Authorize."""))
                // El candado aplica a todo; los endpoints públicos igual funcionan sin token.
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
