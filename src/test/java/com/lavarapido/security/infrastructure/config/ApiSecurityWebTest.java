package com.lavarapido.security.infrastructure.config;

import com.lavarapido.security.domain.exception.EmailAlreadyRegisteredException;
import com.lavarapido.security.domain.exception.InvalidCredentialsException;
import com.lavarapido.security.domain.model.PageResult;
import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.port.in.AuthenticateUserUseCase;
import com.lavarapido.security.domain.port.in.AuthenticationResult;
import com.lavarapido.security.domain.port.in.ChangeAccountStatusUseCase;
import com.lavarapido.security.domain.port.in.ChangePasswordUseCase;
import com.lavarapido.security.domain.port.in.CreateUserAccountUseCase;
import com.lavarapido.security.domain.port.in.GetUserPreferencesUseCase;
import com.lavarapido.security.domain.port.in.GetUserProfileUseCase;
import com.lavarapido.security.domain.port.in.ListUserAccountsUseCase;
import com.lavarapido.security.domain.port.in.LogoutUseCase;
import com.lavarapido.security.domain.port.in.RegisterUserUseCase;
import com.lavarapido.security.domain.port.in.RequestPasswordResetUseCase;
import com.lavarapido.security.domain.port.in.ResetPasswordUseCase;
import com.lavarapido.security.domain.port.in.UpdateUserPreferencesUseCase;
import com.lavarapido.security.domain.port.in.UpdateUserProfileUseCase;
import com.lavarapido.security.domain.port.in.UserAccountView;
import com.lavarapido.security.domain.port.in.VerifyPasswordResetCodeUseCase;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El contrato HTTP y las reglas de seguridad de la API, con los casos de uso simulados:
 * endpoints públicos vs protegidos, verificación del JWT, roles y cuerpos de error.
 */
@WebMvcTest(properties = {
        "security.jwt.secret=" + ApiSecurityWebTest.SECRET,
        "security.jwt.issuer=" + ApiSecurityWebTest.ISSUER,
        "security.jwt.audience=" + ApiSecurityWebTest.AUDIENCE,
        "security.jwt.access-token-ttl=1h",
        "security.password.bcrypt-strength=4"
})
@Import({SecurityConfig.class, JwtConfig.class, ProblemDetailsSecurityHandler.class, CorrelationIdFilter.class})
class ApiSecurityWebTest {

    static final String SECRET = "test-secret-with-at-least-thirty-two-bytes!!";
    static final String ISSUER = "lavarapido-security-service";
    static final String AUDIENCE = "lavarapido-api";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean private RegisterUserUseCase registerUser;
    @MockitoBean private AuthenticateUserUseCase authenticateUser;
    @MockitoBean private LogoutUseCase logout;
    @MockitoBean private RequestPasswordResetUseCase requestPasswordReset;
    @MockitoBean private VerifyPasswordResetCodeUseCase verifyPasswordResetCode;
    @MockitoBean private ResetPasswordUseCase resetPassword;
    @MockitoBean private GetUserProfileUseCase getProfile;
    @MockitoBean private UpdateUserProfileUseCase updateProfile;
    @MockitoBean private ChangePasswordUseCase changePassword;
    @MockitoBean private GetUserPreferencesUseCase getPreferences;
    @MockitoBean private UpdateUserPreferencesUseCase updatePreferences;
    @MockitoBean private CreateUserAccountUseCase createAccount;
    @MockitoBean private ListUserAccountsUseCase listAccounts;
    @MockitoBean private ChangeAccountStatusUseCase changeStatus;

    private static final UserAccountView ANA = new UserAccountView(42L, "ana@gmail.com", "1023456789", "Ana",
            "Pérez", "3001234567", Set.of(RoleCode.CLIENT), true, null);

    private String token(long userId, List<String> roles, String issuer, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(AUDIENCE))
                .subject(String.valueOf(userId))
                .issuedAt(expiresAt.minusSeconds(3600))
                .expiresAt(expiresAt)
                .claim("roles", roles)
                .claim("sid", 1L)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private String validToken(List<String> roles) {
        return token(42L, roles, ISSUER, Instant.now().plusSeconds(3600));
    }

    @Test
    void loginIsPublicAndTheTokenResponseIsNeverCached() throws Exception {
        given(authenticateUser.login(any())).willReturn(
                new AuthenticationResult("jwt-value", Instant.now().plusSeconds(3600), ANA));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@gmail.com\",\"password\":\"Lavado2026!\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.accessToken").value("jwt-value"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.roles[0]").value("CLIENT"));
    }

    @Test
    void wrongCredentialsAnswer401WithAStableCode() throws Exception {
        given(authenticateUser.login(any())).willThrow(new InvalidCredentialsException());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@gmail.com\",\"password\":\"Wrong2026!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void invalidRegistrationFieldsAreListed() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentNumber\":\"1023456789\",\"firstName\":\"Ana\",\"lastName\":\"Pérez\","
                                + "\"email\":\"not-an-email\",\"password\":\"Lavado2026!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void registeringATakenEmailIsAConflict() throws Exception {
        given(registerUser.register(any())).willThrow(new EmailAlreadyRegisteredException());

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentNumber\":\"1023456789\",\"firstName\":\"Ana\",\"lastName\":\"Pérez\","
                                + "\"email\":\"ana@gmail.com\",\"password\":\"Lavado2026!\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void forgotPasswordAlwaysAnswers202() throws Exception {
        mvc.perform(post("/api/v1/auth/password/forgot").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie@gmail.com\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    void theProfileRequiresAToken() throws Exception {
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void theProfileIsReadForTheUserInTheToken() throws Exception {
        given(getProfile.getProfile(42L)).willReturn(ANA);

        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + validToken(List.of("CLIENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@gmail.com"));
        verify(getProfile).getProfile(42L);
    }

    @Test
    void anExpiredTokenIsRejected() throws Exception {
        String expired = token(42L, List.of("CLIENT"), ISSUER, Instant.now().minusSeconds(600));

        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenFromAnotherIssuerIsRejected() throws Exception {
        String foreign = token(42L, List.of("ADMIN"), "someone-else", Instant.now().plusSeconds(3600));

        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + foreign))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenSignedWithAnotherKeyIsRejected() throws Exception {
        JwtEncoder attacker = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(
                "another-secret-with-at-least-thirty-two-bytes".getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(ISSUER).audience(List.of(AUDIENCE)).subject("1")
                .expiresAt(Instant.now().plusSeconds(3600)).claim("roles", List.of("ADMIN")).build();
        String forged = attacker.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void administrationIsForbiddenToCustomers() throws Exception {
        mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + validToken(List.of("CLIENT"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void administrationIsAllowedToAdministrators() throws Exception {
        given(listAccounts.listAccounts(any())).willReturn(new PageResult<>(List.of(ANA), 0, 20, 1));

        mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + validToken(List.of("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].email").value("ana@gmail.com"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void aSafeCorrelationIdIsEchoedAndAnUnsafeOneReplaced() throws Exception {
        mvc.perform(post("/api/v1/auth/password/forgot").header("X-Correlation-Id", "abc-123")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"ana@gmail.com\"}"))
                .andExpect(header().string("X-Correlation-Id", "abc-123"));

        mvc.perform(post("/api/v1/auth/password/forgot").header("X-Correlation-Id", "bad\nid")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"ana@gmail.com\"}"))
                .andExpect(header().string("X-Correlation-Id", org.hamcrest.Matchers.not("bad\nid")));
    }
}
