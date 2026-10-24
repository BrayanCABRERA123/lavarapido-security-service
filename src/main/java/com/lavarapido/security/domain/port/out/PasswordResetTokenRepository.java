package com.lavarapido.security.domain.port.out;

import com.lavarapido.security.domain.model.PasswordResetToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository {

    /**
     * Bloquea los códigos de ese usuario hasta que termine la transacción (SEC-05). Dos intentos al
     * mismo tiempo se revisan uno detrás del otro: sin esto cada uno lee el código antes de que el
     * otro lo queme y se pueden probar muchos más que el tope de intentos.
     */
    void lockUser(long userId);

    PasswordResetToken save(PasswordResetToken token);

    /** Todos los códigos del usuario que no se han usado ni vencido en {@code now}. */
    List<PasswordResetToken> findRedeemableByUserId(long userId, Instant now);

    /** El código canjeable pedido más recientemente: solo el último es válido. */
    Optional<PasswordResetToken> findLatestRedeemableByUserId(long userId, Instant now);

    /** El último código pedido, se haya usado, quemado o vencido. */
    Optional<PasswordResetToken> findLatestByUserId(long userId);
}
