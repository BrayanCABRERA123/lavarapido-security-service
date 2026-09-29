package com.lavarapido.security.infrastructure.adapter.out.security;

import com.lavarapido.security.domain.port.out.ResetCodeGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Seis dígitos aleatorios (la pantalla de verificación tiene seis casillas), de un CSPRNG. */
@Component
class SecureRandomResetCodeGenerator implements ResetCodeGenerator {

    private static final int BOUND = 1_000_000;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        return String.format("%06d", random.nextInt(BOUND));
    }
}
