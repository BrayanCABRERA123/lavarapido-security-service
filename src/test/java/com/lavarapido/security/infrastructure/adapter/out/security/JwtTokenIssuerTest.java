package com.lavarapido.security.infrastructure.adapter.out.security;

import com.lavarapido.security.domain.model.DocumentNumber;
import com.lavarapido.security.domain.model.EmailAddress;
import com.lavarapido.security.domain.model.HashedPassword;
import com.lavarapido.security.domain.model.Person;
import com.lavarapido.security.domain.model.PersonName;
import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.model.UserAccount;
import com.lavarapido.security.domain.port.out.TokenIssuer.IssuedToken;
import com.lavarapido.security.infrastructure.config.JwtProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
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

class JwtTokenIssuerTest {

    private static final String SECRET = "test-secret-with-at-least-thirty-two-bytes!!";
    private static final SecretKey KEY = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    private final JwtProperties properties =
            new JwtProperties(SECRET, "lavarapido-security-service", "lavarapido-api", Duration.ofHours(1));
    private final JwtTokenIssuer issuer = new JwtTokenIssuer(new NimbusJwtEncoder(new ImmutableSecret<>(KEY)), properties);

    private static UserAccount operatorAndClient() {
        Person person = Person.reconstitute(10L, DocumentNumber.of("1023456789"), PersonName.of("Ana", "Pérez"),
                null, EmailAddress.of("ana@gmail.com"));
        return UserAccount.reconstitute(42L, person, EmailAddress.of("ana@gmail.com"), new HashedPassword("hash"),
                true, null, Set.of(RoleCode.OPERATOR, RoleCode.CLIENT));
    }

    @Test
    void issuesAnHs256TokenWithTheAgreedClaims() {
        Instant issuedAt = Instant.now();

        IssuedToken token = issuer.issue(operatorAndClient(), 7L, issuedAt);
        Jwt jwt = NimbusJwtDecoder.withSecretKey(KEY).macAlgorithm(MacAlgorithm.HS256).build().decode(token.value());

        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("CLIENT", "OPERATOR");
        assertThat(jwt.<Number>getClaim("sid").longValue()).isEqualTo(7L);
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("lavarapido-security-service");
        assertThat(jwt.getAudience()).containsExactly("lavarapido-api");
        assertThat(jwt.getId()).isNotBlank();
        assertThat(token.expiresAt()).isEqualTo(issuedAt.plus(Duration.ofHours(1)));
    }

    @Test
    void carriesNoPersonalData() {
        IssuedToken token = issuer.issue(operatorAndClient(), 7L, Instant.now());
        Jwt jwt = NimbusJwtDecoder.withSecretKey(KEY).macAlgorithm(MacAlgorithm.HS256).build().decode(token.value());

        assertThat(jwt.getClaims()).doesNotContainKeys("email", "name", "document", "phone");
        assertThat(jwt.getClaims().values().toString()).doesNotContain("ana@gmail.com", "1023456789");
    }

    @Test
    void refusesToStartWithAShortSecret() {
        assertThatThrownBy(() -> new JwtProperties("too-short", "iss", "aud", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
