package com.lavarapido.security.infrastructure.adapter.out.messaging;

import com.lavarapido.security.domain.event.DomainEvent;
import com.lavarapido.security.domain.event.UserRegistered;
import com.lavarapido.security.domain.port.out.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Publica los eventos de dominio en el exchange carwash.events (ADR-004, cross-cutting.md §7).
 *
 * Reglas de ADR-004:
 * - se publica DESPUÉS de que la transacción de la base se confirma: si el registro falla, nadie
 *   se entera de un usuario que no existe;
 * - cada mensaje lleva un eventId nuevo: los consumidores lo usan para no procesarlo dos veces.
 *
 * Solo se publican los eventos que tienen un consumidor documentado en cross-cutting.md (hoy,
 * UserRegistered: customer-service y notification-service). Los demás siguen yendo al log.
 * Si RabbitMQ está caído, el registro del usuario no falla: se deja el error en el log
 * (el Outbox que evitaría perderlo no está adoptado todavía, ADR-004).
 */
@Component
@ConditionalOnProperty(prefix = "app.messaging", name = "enabled", havingValue = "true")
class RabbitDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitDomainEventPublisher.class);
    private static final int CONTRACT_VERSION = 1;

    private final RabbitTemplate rabbit;
    private final JsonMapper json = JsonMapper.builder().build();

    RabbitDomainEventPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @Override
    public void publish(DomainEvent event) {
        Map<String, Object> payload = payloadOf(event);
        if (payload == null) {
            log.info("Domain event {} has no documented consumer yet, not published: {}", event.eventType(), event);
            return;
        }
        Runnable send = () -> send(event, payload);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    private void send(DomainEvent event, Map<String, Object> payload) {
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("eventType", event.getClass().getSimpleName());
        envelope.put("aggregateId", String.valueOf(payload.get("userId")));
        envelope.put("occurredAt", event.occurredAt().toString());
        envelope.put("version", CONTRACT_VERSION);
        envelope.put("payload", payload);

        try {
            Message message = MessageBuilder.withBody(json.writeValueAsBytes(envelope))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding("UTF-8")
                    .setMessageId(eventId)
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .build();
            rabbit.send(RabbitMessagingConfig.EXCHANGE, event.eventType(), message);
            log.info("Published {} ({}) with routing key {}", envelope.get("eventType"), eventId, event.eventType());
        } catch (AmqpException e) {
            log.error("Could not publish {} ({}) to RabbitMQ: {}", event.eventType(), eventId, e.getMessage());
        }
    }

    /**
     * Payload de cada evento publicado: ids y lo mínimo que un consumidor documentado necesita
     * (ADR-011: correo y primer nombre para el correo de bienvenida). null = evento que todavía
     * no se publica.
     */
    private static Map<String, Object> payloadOf(DomainEvent event) {
        if (event instanceof UserRegistered registered) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("userId", registered.userId());
            payload.put("personId", registered.personId());
            payload.put("email", registered.email());
            payload.put("firstName", registered.firstName());
            payload.put("roles", registered.roles().stream().map(Enum::name).sorted().toList());
            payload.put("createdByAdmin", registered.createdByAdmin());
            return payload;
        }
        return null;
    }
}
