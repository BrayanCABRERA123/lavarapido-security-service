package com.lavarapido.security.infrastructure.adapter.out.persistence;

import com.lavarapido.security.domain.model.LoginLockout;
import com.lavarapido.security.domain.port.out.LoginLockoutRepository;
import com.lavarapido.security.infrastructure.adapter.out.persistence.entity.LoginLockoutJpaEntity;
import com.lavarapido.security.infrastructure.adapter.out.persistence.repository.LoginLockoutJpaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Optional;

@Component
class LoginLockoutPersistenceAdapter implements LoginLockoutRepository {

    /**
     * Candado de SQL Server por cuenta, que se suelta solo al terminar la transacción (igual que la
     * agenda de booking-service). Sirve aunque la cuenta aún no tenga fila de fallos.
     * Devuelve >= 0 si lo obtuvo; negativo si se venció la espera (10 s).
     */
    private static final String LOCK_SQL = """
            SET NOCOUNT ON;
            -- el driver de SQL Server abre la transacción de Spring en la base con la primera lectura
            -- de una tabla; sin ella sp_getapplock responde -999
            DECLARE @open BIGINT;
            SELECT TOP (1) @open = user_id FROM [security].login_lockout;
            DECLARE @result INT;
            EXEC @result = sp_getapplock @Resource = :resource, @LockMode = 'Exclusive',
                                         @LockOwner = 'Transaction', @LockTimeout = 10000;
            SELECT @result;
            """;

    private final LoginLockoutJpaRepository lockouts;
    private final EntityManager entityManager;
    private final Clock clock;

    LoginLockoutPersistenceAdapter(LoginLockoutJpaRepository lockouts, EntityManager entityManager, Clock clock) {
        this.lockouts = lockouts;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Override
    public void lock(long userId) {
        Object result = entityManager.createNativeQuery(LOCK_SQL)
                .setParameter("resource", "login-lockout-" + userId)
                .getSingleResult();
        if (result instanceof Number number && number.intValue() < 0) {
            // solo pasa si otro intento de la misma cuenta se quedó pegado más de 10 s
            throw new IllegalStateException("Could not lock the login attempts of user " + userId);
        }
    }

    @Override
    public Optional<LoginLockout> findByUserId(long userId) {
        return lockouts.findById(userId).map(e -> new LoginLockout(e.getUserId(),
                e.getFailedCount() == null ? 0 : e.getFailedCount(), e.getFirstFailedAt(), e.getLockedUntil()));
    }

    @Override
    public void save(LoginLockout lockout) {
        LoginLockoutJpaEntity entity = lockouts.findById(lockout.userId())
                .orElseGet(() -> new LoginLockoutJpaEntity(lockout.userId()));
        entity.setFailedCount(lockout.failedCount());
        entity.setFirstFailedAt(lockout.firstFailedAt());
        entity.setLockedUntil(lockout.lockedUntil());
        entity.setUpdatedAt(clock.instant());
        lockouts.save(entity);
    }

    @Override
    public void clear(long userId) {
        if (lockouts.existsById(userId)) {
            lockouts.deleteById(userId);
        }
    }
}
