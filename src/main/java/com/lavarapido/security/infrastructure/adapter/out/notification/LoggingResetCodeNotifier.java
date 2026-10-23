package com.lavarapido.security.infrastructure.adapter.out.notification;

import com.lavarapido.security.domain.model.EmailAddress;
import com.lavarapido.security.domain.model.PersonName;
import com.lavarapido.security.domain.port.out.ResetCodeNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * SOLO DESARROLLO: escribe el código de recuperación en el log del servicio en vez de enviarlo por
 * correo, para probar el flujo completo sin servidor SMTP. Nunca se activa fuera del perfil dev,
 * porque un código en un log de producción vale lo mismo que la contraseña.
 */
@Component
@Profile("dev")
// Solo cuando el envío por correo está apagado (app.mail.enabled=false).
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "false", matchIfMissing = true)
class LoggingResetCodeNotifier implements ResetCodeNotifier {

    private static final Logger log = LoggerFactory.getLogger(LoggingResetCodeNotifier.class);

    @Override
    public void send(EmailAddress to, PersonName name, String code, Instant expiresAt) {
        log.info("[DEV] Password recovery code for {} ({}): {} (expires {})", to, name.fullName(), code, expiresAt);
    }
}
