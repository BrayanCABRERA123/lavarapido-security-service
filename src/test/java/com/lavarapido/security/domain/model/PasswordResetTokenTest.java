package com.lavarapido.security.domain.model;

import com.lavarapido.security.domain.exception.InvalidResetCodeException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordResetTokenTest {

    private static final Instant REQUESTED = Instant.parse("2026-09-29T15:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(15);

    private final PasswordResetToken token =
            PasswordResetToken.issue(1L, new HashedPassword("hash"), REQUESTED, TTL, "127.0.0.1");

    @Test
    void isRedeemableUntilItExpires() {
        assertThat(token.isRedeemableAt(REQUESTED.plus(TTL).minusSeconds(1))).isTrue();
        assertThat(token.isRedeemableAt(REQUESTED.plus(TTL))).isFalse();
    }

    @Test
    void canBeRedeemedOnlyOnce() {
        token.redeem(REQUESTED.plusSeconds(60));

        assertThat(token.usedAt()).isEqualTo(REQUESTED.plusSeconds(60));
        assertThatThrownBy(() -> token.redeem(REQUESTED.plusSeconds(61)))
                .isInstanceOf(InvalidResetCodeException.class);
    }

    @Test
    void anExpiredCodeCannotBeRedeemed() {
        assertThatThrownBy(() -> token.redeem(REQUESTED.plus(TTL).plusSeconds(1)))
                .isInstanceOf(InvalidResetCodeException.class);
    }

    @Test
    void invalidatingKeepsTheFirstUsageTime() {
        token.invalidate(REQUESTED.plusSeconds(10));
        token.invalidate(REQUESTED.plusSeconds(20));

        assertThat(token.usedAt()).isEqualTo(REQUESTED.plusSeconds(10));
        assertThat(token.isRedeemableAt(REQUESTED.plusSeconds(30))).isFalse();
    }

    @Test
    void expiryMustComeAfterTheRequest() {
        assertThatThrownBy(() -> PasswordResetToken.issue(1L, new HashedPassword("hash"), REQUESTED,
                Duration.ZERO, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
