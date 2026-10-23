package com.lavarapido.security.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Habilita {@code @Async}: el envío de correos corre fuera del hilo de la petición, así la
 * respuesta HTTP no espera al servidor SMTP.
 */
@Configuration
@EnableAsync
class AsyncConfig {
}
