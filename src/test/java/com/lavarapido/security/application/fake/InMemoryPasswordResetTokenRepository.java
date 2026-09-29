package com.lavarapido.security.application.fake;

import com.lavarapido.security.domain.model.PasswordResetToken;
import com.lavarapido.security.domain.port.out.PasswordResetTokenRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryPasswordResetTokenRepository implements PasswordResetTokenRepository {

    private final Map<Long, PasswordResetToken> tokens = new LinkedHashMap<>();
    private long nextId = 1;

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
        return findRedeemableByUserId(userId, now).stream()
                .max(Comparator.comparing(PasswordResetToken::requestedAt).thenComparing(PasswordResetToken::id));
    }

    public List<PasswordResetToken> all() {
        return new ArrayList<>(tokens.values());
    }
}
