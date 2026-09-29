package com.lavarapido.security.infrastructure.adapter.out.security;

import com.lavarapido.security.domain.port.out.ResetAttemptCounter;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contador de intentos fallidos en memoria. {@code password_reset_token} no tiene columna de intentos
 * y el esquema compartido no es nuestro para cambiarlo, así que esto basta con una sola instancia. Con más
 * de una réplica debe pasar a Redis (la caché del gateway) o a una columna nueva.
 */
@Component
class InMemoryResetAttemptCounter implements ResetAttemptCounter {

    /** Más que la vida de cualquier código, así nunca se borra una entrada mientras su código siga vigente. */
    private static final Duration RETENTION = Duration.ofHours(1);

    private record Attempts(int failures, Instant firstFailureAt) {
    }

    private final Map<Long, Attempts> attemptsByToken = new ConcurrentHashMap<>();
    private final Clock clock;

    InMemoryResetAttemptCounter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public int recordFailure(long tokenId) {
        Instant now = clock.instant();
        attemptsByToken.values().removeIf(attempts -> attempts.firstFailureAt().plus(RETENTION).isBefore(now));
        return attemptsByToken.merge(tokenId, new Attempts(1, now),
                (current, ignored) -> new Attempts(current.failures() + 1, current.firstFailureAt())).failures();
    }

    @Override
    public void clear(long tokenId) {
        attemptsByToken.remove(tokenId);
    }
}
