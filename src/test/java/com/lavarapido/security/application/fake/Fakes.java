package com.lavarapido.security.application.fake;

import com.lavarapido.security.domain.event.DomainEvent;
import com.lavarapido.security.domain.model.EmailAddress;
import com.lavarapido.security.domain.model.HashedPassword;
import com.lavarapido.security.domain.model.PersonName;
import com.lavarapido.security.domain.model.UserAccount;
import com.lavarapido.security.domain.port.out.DomainEventPublisher;
import com.lavarapido.security.domain.port.out.PasswordHasher;
import com.lavarapido.security.domain.port.out.ResetAttemptCounter;
import com.lavarapido.security.domain.port.out.ResetCodeGenerator;
import com.lavarapido.security.domain.port.out.ResetCodeNotifier;
import com.lavarapido.security.domain.port.out.TokenIssuer;
import com.lavarapido.security.domain.port.out.UserSessionRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Dobles de prueba escritos a mano para los puertos de salida. */
public final class Fakes {

    private Fakes() {
    }

    /** "Hash" legible y determinista: sirve para probar los casos de uso, nunca para producción. */
    public static class PlainPasswordHasher implements PasswordHasher {

        @Override
        public HashedPassword hash(String raw) {
            return new HashedPassword("hashed:" + raw);
        }

        @Override
        public boolean matches(String raw, HashedPassword hash) {
            return raw != null && hash.value().equals("hashed:" + raw);
        }
    }

    public static class MutableClock extends Clock {

        private Instant now;

        public MutableClock(Instant now) {
            this.now = now;
        }

        public void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    public static class RecordingEventPublisher implements DomainEventPublisher {

        public final List<DomainEvent> events = new ArrayList<>();

        @Override
        public void publish(DomainEvent event) {
            events.add(event);
        }
    }

    public static class FixedResetCodeGenerator implements ResetCodeGenerator {

        public String nextCode = "123456";

        @Override
        public String generate() {
            return nextCode;
        }
    }

    public static class RecordingResetCodeNotifier implements ResetCodeNotifier {

        public final Map<String, String> lastCodeByEmail = new HashMap<>();
        public int sent;

        @Override
        public void send(EmailAddress to, PersonName name, String code, Instant expiresAt) {
            lastCodeByEmail.put(to.value(), code);
            sent++;
        }
    }

    public static class InMemoryResetAttemptCounter implements ResetAttemptCounter {

        private final Map<Long, Integer> failures = new HashMap<>();

        @Override
        public int recordFailure(long tokenId) {
            return failures.merge(tokenId, 1, Integer::sum);
        }

        @Override
        public void clear(long tokenId) {
            failures.remove(tokenId);
        }
    }

    public static class InMemoryUserSessionRepository implements UserSessionRepository {

        public record Session(long userId, Instant startedAt, Instant expiresAt, String ipAddress, Instant revokedAt) {
        }

        public final Map<Long, Session> sessions = new LinkedHashMap<>();
        private long nextId = 1;

        @Override
        public long open(long userId, Instant startedAt, Instant expiresAt, String ipAddress, String userAgent) {
            long id = nextId++;
            sessions.put(id, new Session(userId, startedAt, expiresAt, ipAddress, null));
            return id;
        }

        @Override
        public void revoke(long sessionId, long userId, Instant revokedAt) {
            Session session = sessions.get(sessionId);
            if (session != null && session.userId() == userId && session.revokedAt() == null) {
                sessions.put(sessionId, new Session(userId, session.startedAt(), session.expiresAt(),
                        session.ipAddress(), revokedAt));
            }
        }
    }

    public static class FakeTokenIssuer implements TokenIssuer {

        @Override
        public Duration timeToLive() {
            return Duration.ofHours(1);
        }

        @Override
        public IssuedToken issue(UserAccount account, long sessionId, Instant issuedAt) {
            return new IssuedToken("token-for-" + account.id() + "-session-" + sessionId,
                    issuedAt.plus(timeToLive()));
        }
    }
}
