package com.lavarapido.security.infrastructure.adapter.in.web;

import org.springframework.security.oauth2.jwt.Jwt;

/** Quién hace la llamada, leído de un token de acceso ya verificado. */
record AuthenticatedUser(long userId, long sessionId) {

    static AuthenticatedUser from(Jwt jwt) {
        Number sessionId = jwt.getClaim("sid");
        return new AuthenticatedUser(Long.parseLong(jwt.getSubject()), sessionId == null ? 0L : sessionId.longValue());
    }
}
