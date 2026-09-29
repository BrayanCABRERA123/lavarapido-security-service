package com.lavarapido.security.domain.port.out;

import com.lavarapido.security.domain.model.EmailAddress;
import com.lavarapido.security.domain.model.PersonName;

import java.time.Instant;

/** Le entrega el código de recuperación a su dueño (correo/SMS). */
public interface ResetCodeNotifier {

    void send(EmailAddress to, PersonName name, String code, Instant expiresAt);
}
