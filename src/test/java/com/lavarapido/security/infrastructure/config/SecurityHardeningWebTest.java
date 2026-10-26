package com.lavarapido.security.infrastructure.config;

import com.lavarapido.security.domain.port.in.AuthenticateUserUseCase;
import com.lavarapido.security.domain.port.in.ChangeAccountStatusUseCase;
import com.lavarapido.security.domain.port.in.ChangeEmailUseCase;
import com.lavarapido.security.domain.port.in.ChangePasswordUseCase;
import com.lavarapido.security.domain.port.in.CreateUserAccountUseCase;
import com.lavarapido.security.domain.port.in.DeactivateOwnAccountUseCase;
import com.lavarapido.security.domain.port.in.GetUserPreferencesUseCase;
import com.lavarapido.security.domain.port.in.GetUserProfileUseCase;
import com.lavarapido.security.domain.port.in.ListRolePermissionsUseCase;
import com.lavarapido.security.domain.port.in.ListUserAccountsUseCase;
import com.lavarapido.security.domain.port.in.LogoutUseCase;
import com.lavarapido.security.domain.port.in.PersonAddressUseCase;
import com.lavarapido.security.domain.port.in.RegisterUserUseCase;
import com.lavarapido.security.domain.port.in.RequestPasswordResetUseCase;
import com.lavarapido.security.domain.port.in.ResetPasswordUseCase;
import com.lavarapido.security.domain.port.in.SessionStatusUseCase;
import com.lavarapido.security.domain.port.in.UpdateRolePermissionsUseCase;
import com.lavarapido.security.domain.port.in.UpdateUserPreferencesUseCase;
import com.lavarapido.security.domain.port.in.UpdateUserProfileUseCase;
import com.lavarapido.security.domain.port.in.VerifyPasswordResetCodeUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de seguridad HTTP y endurecimiento de la API (Security Hardening):
 * - Encabezados de seguridad e inyección en correlation IDs.
 * - Filtro de llaves internas de comunicación entre microservicios (/internal/**).
 * - Protección CORS frente a orígenes no autorizados.
 * - Validación y sanitización de cargas maliciosas.
 */
@WebMvcTest(properties = {
        "security.jwt.secret=test-secret-with-at-least-thirty-two-bytes!!",
        "security.jwt.issuer=lavarapido-security-service",
        "security.jwt.audience=lavarapido-api",
        "app.internal.api-key=internal-test-key-with-enough-length",
        "app.cors.allowed-origins=http://localhost:3000"
})
@Import({SecurityConfig.class, JwtConfig.class, ProblemDetailsSecurityHandler.class, CorrelationIdFilter.class})
class SecurityHardeningWebTest {

    static final String INTERNAL_KEY = "internal-test-key-with-enough-length";

    @Autowired
    private MockMvc mvc;

    @MockitoBean private RegisterUserUseCase registerUser;
    @MockitoBean private AuthenticateUserUseCase authenticateUser;
    @MockitoBean private LogoutUseCase logout;
    @MockitoBean private SessionStatusUseCase sessionStatus;
    @MockitoBean private RequestPasswordResetUseCase requestPasswordReset;
    @MockitoBean private VerifyPasswordResetCodeUseCase verifyPasswordResetCode;
    @MockitoBean private ResetPasswordUseCase resetPassword;
    @MockitoBean private GetUserProfileUseCase getProfile;
    @MockitoBean private UpdateUserProfileUseCase updateProfile;
    @MockitoBean private ChangePasswordUseCase changePassword;
    @MockitoBean private GetUserPreferencesUseCase getPreferences;
    @MockitoBean private UpdateUserPreferencesUseCase updatePreferences;
    @MockitoBean private PersonAddressUseCase addresses;
    @MockitoBean private CreateUserAccountUseCase createAccount;
    @MockitoBean private ListUserAccountsUseCase listAccounts;
    @MockitoBean private ChangeAccountStatusUseCase changeStatus;
    @MockitoBean private DeactivateOwnAccountUseCase deactivateOwnAccount;
    @MockitoBean private ChangeEmailUseCase changeEmail;
    @MockitoBean private ListRolePermissionsUseCase listRolePermissions;
    @MockitoBean private UpdateRolePermissionsUseCase updateRolePermissions;

    @Test
    void securityHeadersArePresentOnResponses() throws Exception {
        mvc.perform(post("/api/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@gmail.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void internalEndpointsRejectRequestsWithoutOrWithInvalidKey() throws Exception {
        // Sin encabezado
        mvc.perform(get("/internal/v1/users/42/contact"))
                .andExpect(status().isUnauthorized());

        // Llave vacía
        mvc.perform(get("/internal/v1/users/42/contact").header("X-Internal-Key", ""))
                .andExpect(status().isUnauthorized());

        // Llave incorrecta de longitud similar
        mvc.perform(get("/internal/v1/users/42/contact").header("X-Internal-Key", "internal-test-key-with-wrong--length"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void correlationIdFilterRejectsLogInjectionAttemptAndGeneratesCleanUuid() throws Exception {
        String crlfInjection = "id-123\r\nSET-COOKIE: malicious=true\nINJECTED-LOG";

        mvc.perform(post("/api/v1/auth/password/forgot")
                        .header("X-Correlation-Id", crlfInjection)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@gmail.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(header().string("X-Correlation-Id", not(crlfInjection)));
    }

    @Test
    void correlationIdFilterAcceptsValidCorrelationId() throws Exception {
        String validId = "req-uuid-98765-xyz";

        mvc.perform(post("/api/v1/auth/password/forgot")
                        .header("X-Correlation-Id", validId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@gmail.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(header().string("X-Correlation-Id", validId));
    }

    @Test
    void corsOptionsRequestAllowedForConfiguredOrigin() throws Exception {
        mvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void corsOptionsRequestDeniedForUnregisteredOrigin() throws Exception {
        mvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://evil-attacker-site.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registrationRejectsMalformedInputPayloads() throws Exception {
        // Formato de email malicioso o inválido
        String payload = "{\"documentNumber\":\"123456789\",\"firstName\":\"User\",\"lastName\":\"Test\","
                + "\"email\":\"<script>alert(1)</script>\",\"password\":\"Lavado2026!\"}";

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
