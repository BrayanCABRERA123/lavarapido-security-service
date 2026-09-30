package com.lavarapido.security.infrastructure.adapter.out.persistence;

import com.lavarapido.security.domain.port.out.UserSessionRepository;
import com.lavarapido.security.infrastructure.adapter.out.persistence.entity.UserSessionJpaEntity;
import com.lavarapido.security.infrastructure.adapter.out.persistence.repository.UserSessionJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
class UserSessionPersistenceAdapter implements UserSessionRepository {

    /** Anchos de columna de {@code security.user_session}. */
    private static final int MAX_IP_LENGTH = 45;
    private static final int MAX_USER_AGENT_LENGTH = 300;

    private final UserSessionJpaRepository sessions;

    UserSessionPersistenceAdapter(UserSessionJpaRepository sessions) {
        this.sessions = sessions;
    }

    @Override
    public long open(long userId, Instant startedAt, Instant expiresAt, String ipAddress, String userAgent) {
        UserSessionJpaEntity session = new UserSessionJpaEntity(userId, startedAt, expiresAt,
                truncate(ipAddress, MAX_IP_LENGTH), truncate(userAgent, MAX_USER_AGENT_LENGTH));
        return sessions.save(session).getId();
    }

    @Override
    public void revoke(long sessionId, long userId, Instant revokedAt) {
        sessions.findByIdAndUserId(sessionId, userId)
                .filter(session -> session.getRevokedAt() == null)
                .ifPresent(session -> session.setRevokedAt(revokedAt));
    }

    @Override
    public void revokeAll(long userId, Instant revokedAt) {
        sessions.findByUserIdAndRevokedAtIsNull(userId)
                .forEach(session -> session.setRevokedAt(revokedAt));
    }

    private static String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
