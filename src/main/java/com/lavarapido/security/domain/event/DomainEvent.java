package com.lavarapido.security.domain.event;

import java.time.Instant;

/** Algo que pasó en el contexto de identidad y que puede interesarle a otros contextos. */
public interface DomainEvent {

    /** Routing key con el que se publica el evento (cross-cutting.md §7), ej. {@code security.user_registered}. */
    String eventType();

    Instant occurredAt();
}
