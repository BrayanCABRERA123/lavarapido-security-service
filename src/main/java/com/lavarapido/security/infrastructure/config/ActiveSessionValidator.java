package com.lavarapido.security.infrastructure.config;

import com.lavarapido.security.domain.port.in.SessionStatusUseCase;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Rechaza el token si su sesión (claim {@code sid}) ya se cerró: logout, cuenta inhabilitada por el
 * admin o desactivada por el dueño. Sin esto el token seguía sirviendo hasta su exp (1 hora).
 * El gateway pregunta lo mismo a GET /api/v1/auth/session antes de pasar a los demás servicios.
 *
 * Todo token que entrega el login trae su sesión. Uno sin {@code sid} solo lo puede firmar quien
 * tiene el secreto (las pruebas e2e, por ejemplo), y ese igual podría poner el sid de una sesión
 * abierta: exigirlo no protege de nada más, así que esos tokens no se atan a una sesión.
 */
final class ActiveSessionValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error SESSION_CLOSED =
            new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "La sesión de este token ya se cerró", null);

    private final SessionStatusUseCase sessions;

    ActiveSessionValidator(SessionStatusUseCase sessions) {
        this.sessions = sessions;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Object sessionId = jwt.getClaims().get("sid");
        if (sessionId == null) {
            return OAuth2TokenValidatorResult.success();
        }
        long userId;
        try {
            userId = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException | NullPointerException notAUser) {
            return OAuth2TokenValidatorResult.failure(SESSION_CLOSED);
        }
        if (!(sessionId instanceof Number sid) || !sessions.isActive(userId, sid.longValue())) {
            return OAuth2TokenValidatorResult.failure(SESSION_CLOSED);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
