package com.lavarapido.security.infrastructure.adapter.out.security;

import com.lavarapido.security.domain.model.DocumentNumber;
import com.lavarapido.security.domain.model.EmailAddress;
import com.lavarapido.security.domain.model.HashedPassword;
import com.lavarapido.security.domain.model.Person;
import com.lavarapido.security.domain.model.PersonName;
import com.lavarapido.security.domain.model.PhoneNumber;
import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.model.UserAccount;
import com.lavarapido.security.domain.port.out.TokenIssuer.IssuedToken;
import com.lavarapido.security.infrastructure.config.JwtProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de seguridad específicas para JWT:
 * - Firma y falsificación de tokens.
 * - Validación de algoritmos permitidos (HS256).
 * - Expiración y límites temporales.
 * - Ausencia de datos personales (PII) en los claims.
 */
class JwtSecurityValidationTest {

    private static final String SECRET = "test-secret-with-at-least-thirty-two-bytes!!";
    private static final String WRONG_SECRET = "wrong-secret-with-at-least-thirty-two-bytes!!";
    private static final SecretKey KEY = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    private final JwtProperties properties =
            new JwtProperties(SECRET, "lavarapido-security-service", "lavarapido-api", Duration.ofHours(1));
    private final JwtTokenIssuer issuer = new JwtTokenIssuer(new NimbusJwtEncoder(new ImmutableSecret<>(KEY)), properties);

    private static UserAccount sampleAccount() {
        Person person = Person.reconstitute(100L, DocumentNumber.of("1023456789"), PersonName.of("Carlos", "Gómez"),
                PhoneNumber.ofNullable("3109876543"), EmailAddress.of("carlos@gmail.com"));
        return UserAccount.reconstitute(50L, person, EmailAddress.of("carlos@gmail.com"), new HashedPassword("hash"),
                true, null, Set.of(RoleCode.CLIENT));
    }

    @Test
    void decoderRejectsTokenSignedWithDifferentSecret() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(KEY).macAlgorithm(MacAlgorithm.HS256).build();

        // Generar token con un secreto diferente
        SecretKey wrongKey = new SecretKeySpec(WRONG_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtProperties wrongProperties = new JwtProperties(WRONG_SECRET, "lavarapido-security-service", "lavarapido-api", Duration.ofHours(1));
        JwtTokenIssuer wrongIssuer = new JwtTokenIssuer(new NimbusJwtEncoder(new ImmutableSecret<>(wrongKey)), wrongProperties);

        IssuedToken forgedToken = wrongIssuer.issue(sampleAccount(), 10L, Instant.now());

        assertThatThrownBy(() -> decoder.decode(forgedToken.value()))
                .isInstanceOf(BadJwtException.class);
    }

    @Test
    void decoderRejectsExpiredToken() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(KEY).macAlgorithm(MacAlgorithm.HS256).build();

        // Emitir token con fecha pasada
        Instant past = Instant.now().minus(Duration.ofHours(2));
        IssuedToken expiredToken = issuer.issue(sampleAccount(), 10L, past);

        assertThatThrownBy(() -> decoder.decode(expiredToken.value()))
                .isInstanceOf(BadJwtException.class);
    }

    @Test
    void jwtContainsOnlyStrictlyRequiredClaimsWithoutPersonalData() {
        IssuedToken token = issuer.issue(sampleAccount(), 10L, Instant.now());
        Jwt jwt = NimbusJwtDecoder.withSecretKey(KEY).macAlgorithm(MacAlgorithm.HS256).build().decode(token.value());

        // Debe contener claims esenciales
        assertThat(jwt.getSubject()).isEqualTo("50");
        assertThat(jwt.<Number>getClaim("sid").longValue()).isEqualTo(10L);
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("CLIENT");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("lavarapido-security-service");

        // NO debe exponer datos personales (PII)
        assertThat(jwt.getClaims()).doesNotContainKeys("email", "firstName", "lastName", "documentNumber", "phone");
    }

    @Test
    void decoderRejectsMalformedOrTamperedJwtString() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(KEY).macAlgorithm(MacAlgorithm.HS256).build();

        String tamperedJwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.invalid_signature";

        assertThatThrownBy(() -> decoder.decode(tamperedJwt))
                .isInstanceOf(BadJwtException.class);
    }
}
