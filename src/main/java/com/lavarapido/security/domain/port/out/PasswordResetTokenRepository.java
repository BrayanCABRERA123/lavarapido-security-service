package com.lavarapido.security.domain.port.out;

import com.lavarapido.security.domain.model.PasswordResetToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository {

    PasswordResetToken save(PasswordResetToken token);

    /** Todos los códigos del usuario que no se han usado ni vencido en {@code now}. */
    List<PasswordResetToken> findRedeemableByUserId(long userId, Instant now);

    /** El código canjeable pedido más recientemente: solo el último es válido. */
    Optional<PasswordResetToken> findLatestRedeemableByUserId(long userId, Instant now);
}
