package com.lavarapido.security.infrastructure.config;

import com.lavarapido.security.domain.model.AllowedEmailDomains;
import com.lavarapido.security.domain.service.PasswordPolicy;
import com.lavarapido.security.domain.service.PasswordRecoveryPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

/** Registra los objetos del dominio, que no llevan anotaciones de Spring. */
@Configuration
class DomainConfig {

    /** Toda fecha y hora va en UTC (06-data/modeling-conventions.md). */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /** A-01: dominios de correo permitidos (ALLOWED_EMAIL_DOMAINS, por defecto gmail.com). */
    @Bean
    AllowedEmailDomains allowedEmailDomains(@Value("${app.allowed-email-domains:gmail.com}") String domains) {
        return AllowedEmailDomains.parse(domains);
    }

    @Bean
    PasswordPolicy passwordPolicy() {
        return new PasswordPolicy();
    }

    @Bean
    PasswordRecoveryPolicy passwordRecoveryPolicy(
            @Value("${security.password-recovery.code-ttl:15m}") Duration codeTimeToLive,
            @Value("${security.password-recovery.max-failed-attempts:5}") int maxFailedAttempts) {
        return new PasswordRecoveryPolicy(codeTimeToLive, maxFailedAttempts);
    }
}
