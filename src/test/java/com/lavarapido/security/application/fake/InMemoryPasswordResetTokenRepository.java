package com.lavarapido.security.application.fake;

import com.lavarapido.security.domain.model.PasswordResetToken;
import com.lavarapido.security.domain.port.out.PasswordResetTokenRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class InMemoryPasswordResetTokenRepository implements PasswordResetTokenRepository {

    private final Map<Long, PasswordResetToken> tokens = new LinkedHashMap<>();
    private long nextId = 1;
    /** usuarios bloqueados en la transacción actual; buscar el último código sin bloquear se anota en unlockedReads */
    public final Set<Long> locked = new HashSet<>();
    public int unlockedReads;

    @Override
    public void lockUser(long userId) {
        locked.add(userId);
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        long id = token.id() == null ? nextId++ : token.id();
        PasswordResetToken stored = PasswordResetToken.reconstitute(id, token.userId(), token.codeHash(),
                token.requestedAt(), token.expiresAt(), token.usedAt(), token.ipAddress());
        tokens.put(id, stored);
        return stored;
    }

    @Override
    public List<PasswordResetToken> findRedeemableByUserId(long userId, Instant now) {
        return tokens.values().stream()
                .filter(token -> token.userId() == userId && token.isRedeemableAt(now))
                .toList();
    }

    @Override
    public Optional<PasswordResetToken> findLatestRedeemableByUserId(long userId, Instant now) {
        if (!locked.contains(userId)) {
            unlockedReads++;
        }
        return findRedeemableByUserId(userId, now).stream()
                .max(Comparator.comparing(PasswordResetToken::requestedAt).thenComparing(PasswordResetToken::id));
    }

    @Override
    public Optional<PasswordResetToken> findLatestByUserId(long userId) {
        return tokens.values().stream()
                .filter(token -> token.userId() == userId)
                .max(Comparator.comparing(PasswordResetToken::requestedAt).thenComparing(PasswordResetToken::id));
    }

    public List<PasswordResetToken> all() {
        return new ArrayList<>(tokens.values());
    }
}
