package com.lavarapido.security.infrastructure.adapter.out.messaging;

import com.lavarapido.security.domain.event.DomainEvent;
import com.lavarapido.security.domain.port.out.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Se usa cuando la mensajería está apagada (app.messaging.enabled=false, el valor por defecto):
 * los eventos solo quedan en el log y el servicio funciona sin RabbitMQ. Con la mensajería
 * encendida los publica RabbitDomainEventPublisher; ningún caso de uso cambia.
 */
@Component
@ConditionalOnProperty(prefix = "app.messaging", name = "enabled", havingValue = "false", matchIfMissing = true)
class LoggingDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingDomainEventPublisher.class);

    @Override
    public void publish(DomainEvent event) {
        log.info("Domain event {}: {}", event.eventType(), event);
    }
}
