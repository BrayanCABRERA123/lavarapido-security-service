package com.lavarapido.security.infrastructure.adapter.out.security;

import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.model.UserAccount;
import com.lavarapido.security.domain.port.out.TokenIssuer;
import com.lavarapido.security.infrastructure.config.JwtProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Firma los tokens de acceso con HS256 (ADR-006). Los claims se limitan a lo que necesita la autorización:
 * id del usuario ({@code sub}), roles, id de sesión y los claims de tiempo estándar. Sin datos personales.
 */
@Component
class JwtTokenIssuer implements TokenIssuer {

    static final String ROLES_CLAIM = "roles";
    static final String SESSION_CLAIM = "sid";

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    JwtTokenIssuer(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    @Override
    public Duration timeToLive() {
        return properties.accessTokenTtl();
    }

    @Override
    public IssuedToken issue(UserAccount account, long sessionId, Instant issuedAt) {
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        List<String> roles = account.roles().stream().map(RoleCode::name).sorted().toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.issuer())
                .audience(List.of(properties.audience()))
                .subject(String.valueOf(account.id()))
                .issuedAt(issuedAt)
                .notBefore(issuedAt)
                .expiresAt(expiresAt)
                .claim(ROLES_CLAIM, roles)
                .claim(SESSION_CLAIM, sessionId)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();

        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }
}
