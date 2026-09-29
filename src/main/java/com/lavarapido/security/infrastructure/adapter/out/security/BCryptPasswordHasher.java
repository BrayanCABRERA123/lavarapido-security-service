package com.lavarapido.security.infrastructure.adapter.out.security;

import com.lavarapido.security.domain.model.HashedPassword;
import com.lavarapido.security.domain.port.out.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** BCrypt a través del {@link PasswordEncoder} de Spring Security (ADR-006). */
@Component
class BCryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder;

    BCryptPasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public HashedPassword hash(String raw) {
        return new HashedPassword(encoder.encode(raw));
    }

    @Override
    public boolean matches(String raw, HashedPassword hash) {
        return raw != null && encoder.matches(raw, hash.value());
    }
}
