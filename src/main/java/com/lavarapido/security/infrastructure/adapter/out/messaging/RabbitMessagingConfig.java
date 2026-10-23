package com.lavarapido.security.infrastructure.adapter.out.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara el exchange compartido carwash.events (cross-cutting.md §7) por si este servicio
 * arranca antes que los consumidores. Las colas las declara cada servicio que consume.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.messaging", name = "enabled", havingValue = "true")
class RabbitMessagingConfig {

    static final String EXCHANGE = "carwash.events";

    @Bean
    TopicExchange carwashEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }
}
