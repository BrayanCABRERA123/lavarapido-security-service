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
 * Reemplazo fuera de dev mientras se elige un proveedor de correo/SMS: registra que se emitió un
 * código sin escribir nunca el código.
 */
@Component
@Profile("!dev")
// Solo cuando el envío por correo está apagado (app.mail.enabled=false).
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "false", matchIfMissing = true)
class UndeliveredResetCodeNotifier implements ResetCodeNotifier {

    private static final Logger log = LoggerFactory.getLogger(UndeliveredResetCodeNotifier.class);

    @Override
    public void send(EmailAddress to, PersonName name, String code, Instant expiresAt) {
        log.warn("Password recovery code issued but no delivery channel is configured (expires {})", expiresAt);
    }
}
