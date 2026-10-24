package com.lavarapido.security.infrastructure.adapter.out.persistence;

import com.lavarapido.security.domain.model.HashedPassword;
import com.lavarapido.security.domain.model.PasswordResetToken;
import com.lavarapido.security.domain.port.out.PasswordResetTokenRepository;
import com.lavarapido.security.infrastructure.adapter.out.persistence.entity.PasswordResetTokenJpaEntity;
import com.lavarapido.security.infrastructure.adapter.out.persistence.repository.PasswordResetTokenJpaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
class PasswordResetTokenPersistenceAdapter implements PasswordResetTokenRepository {

    /**
     * Candado de SQL Server por usuario, igual al de login_lockout (LoginLockoutPersistenceAdapter):
     * se suelta solo al terminar la transacción. Devuelve >= 0 si lo obtuvo; negativo si se venció la
     * espera (10 s).
     */
    private static final String LOCK_SQL = """
            SET NOCOUNT ON;
            -- el driver de SQL Server abre la transacción de Spring en la base con la primera lectura
            -- de una tabla; sin ella sp_getapplock responde -999
            DECLARE @open BIGINT;
            SELECT TOP (1) @open = password_reset_token_id FROM [security].password_reset_token;
            DECLARE @result INT;
            EXEC @result = sp_getapplock @Resource = :resource, @LockMode = 'Exclusive',
                                         @LockOwner = 'Transaction', @LockTimeout = 10000;
            SELECT @result;
            """;

    private final PasswordResetTokenJpaRepository tokens;
    private final EntityManager entityManager;

    PasswordResetTokenPersistenceAdapter(PasswordResetTokenJpaRepository tokens, EntityManager entityManager) {
        this.tokens = tokens;
        this.entityManager = entityManager;
    }

    @Override
    public void lockUser(long userId) {
        Object result = entityManager.createNativeQuery(LOCK_SQL)
                .setParameter("resource", "password-reset-" + userId)
                .getSingleResult();
        if (result instanceof Number number && number.intValue() < 0) {
            // solo pasa si otro intento del mismo usuario se quedó pegado más de 10 s
            throw new IllegalStateException("Could not lock the reset codes of user " + userId);
        }
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

    @Override
    public Optional<PasswordResetToken> findLatestByUserId(long userId) {
        return tokens.findFirstByUserIdOrderByRequestedAtDescIdDesc(userId)
                .map(PasswordResetTokenPersistenceAdapter::toDomain);
    }

    private static PasswordResetToken toDomain(PasswordResetTokenJpaEntity entity) {
        return PasswordResetToken.reconstitute(entity.getId(), entity.getUserId(),
                new HashedPassword(entity.getTokenHash()), entity.getRequestedAt(), entity.getExpiresAt(),
                entity.getUsedAt(), entity.getIpAddress());
    }
}
