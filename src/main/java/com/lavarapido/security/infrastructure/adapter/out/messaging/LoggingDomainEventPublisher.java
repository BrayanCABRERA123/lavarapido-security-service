package com.lavarapido.security.infrastructure.adapter.out.messaging;

import com.lavarapido.security.domain.event.DomainEvent;
import com.lavarapido.security.domain.port.out.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Reemplazo temporal mientras se conecta RabbitMQ (ADR-004): los eventos ya se lanzan en los
 * lugares correctos, así que cambiar este adaptador no toca ningún caso de uso.
 */
@Component
class LoggingDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingDomainEventPublisher.class);

    @Override
    public void publish(DomainEvent event) {
        log.info("Domain event {}: {}", event.eventType(), event);
    }
}
