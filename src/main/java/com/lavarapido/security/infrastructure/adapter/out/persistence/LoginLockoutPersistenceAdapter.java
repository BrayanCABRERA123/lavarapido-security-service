package com.lavarapido.security.infrastructure.adapter.out.persistence;

import com.lavarapido.security.domain.model.LoginLockout;
import com.lavarapido.security.domain.port.out.LoginLockoutRepository;
import com.lavarapido.security.infrastructure.adapter.out.persistence.entity.LoginLockoutJpaEntity;
import com.lavarapido.security.infrastructure.adapter.out.persistence.repository.LoginLockoutJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Optional;

@Component
class LoginLockoutPersistenceAdapter implements LoginLockoutRepository {

    private final LoginLockoutJpaRepository lockouts;
    private final Clock clock;

    LoginLockoutPersistenceAdapter(LoginLockoutJpaRepository lockouts, Clock clock) {
        this.lockouts = lockouts;
        this.clock = clock;
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
