package com.lavarapido.security.domain.port.out;

import com.lavarapido.security.domain.event.DomainEvent;

public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
