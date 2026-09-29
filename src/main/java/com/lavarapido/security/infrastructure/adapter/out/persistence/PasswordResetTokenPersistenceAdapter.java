package com.lavarapido.security.infrastructure.adapter.out.persistence;

import com.lavarapido.security.domain.model.HashedPassword;
import com.lavarapido.security.domain.model.PasswordResetToken;
import com.lavarapido.security.domain.port.out.PasswordResetTokenRepository;
import com.lavarapido.security.infrastructure.adapter.out.persistence.entity.PasswordResetTokenJpaEntity;
import com.lavarapido.security.infrastructure.adapter.out.persistence.repository.PasswordResetTokenJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
class PasswordResetTokenPersistenceAdapter implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository tokens;

    PasswordResetTokenPersistenceAdapter(PasswordResetTokenJpaRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        PasswordResetTokenJpaEntity entity = token.id() == null
                ? new PasswordResetTokenJpaEntity(token.userId(), token.codeHash().value(), token.requestedAt(),
                        token.expiresAt(), token.ipAddress())
                : tokens.findById(token.id()).orElseThrow(
                        () -> new IllegalStateException("Reset token " + token.id() + " vanished while being updated"));
        entity.setUsedAt(token.usedAt());
        return toDomain(tokens.save(entity));
    }

    @Override
    public List<PasswordResetToken> findRedeemableByUserId(long userId, Instant now) {
        return tokens.findByUserIdAndUsedAtIsNullAndExpiresAtAfter(userId, now).stream()
                .map(PasswordResetTokenPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public Optional<PasswordResetToken> findLatestRedeemableByUserId(long userId, Instant now) {
        return tokens.findFirstByUserIdAndUsedAtIsNullAndExpiresAtAfterOrderByRequestedAtDescIdDesc(userId, now)
                .map(PasswordResetTokenPersistenceAdapter::toDomain);
    }

    private static PasswordResetToken toDomain(PasswordResetTokenJpaEntity entity) {
        return PasswordResetToken.reconstitute(entity.getId(), entity.getUserId(),
                new HashedPassword(entity.getTokenHash()), entity.getRequestedAt(), entity.getExpiresAt(),
                entity.getUsedAt(), entity.getIpAddress());
    }
}
