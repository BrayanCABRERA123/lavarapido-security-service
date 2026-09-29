package com.lavarapido.security.domain.event;

import java.time.Instant;

/** Algo que pasó en el contexto de identidad y que puede interesarle a otros contextos. */
public interface DomainEvent {

    /** Routing key / tópico con el que se publica el evento, ej. {@code security.user.registered}. */
    String eventType();

    Instant occurredAt();
}
