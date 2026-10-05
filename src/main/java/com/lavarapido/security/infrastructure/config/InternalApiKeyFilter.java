package com.lavarapido.security.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Autentica las llamadas entre servicios a /internal/** con la llave compartida
 * (INTERNAL_API_KEY) que viaja en el encabezado X-Internal-Key.
 *
 * Si la llave coincide, la petición queda con el rol INTERNAL. Si no coincide, o si la llave no
 * está configurada, no se autentica y la cadena responde 401: el endpoint queda cerrado.
 * La comparación es en tiempo constante para no dar pistas por cuánto tarda.
 */
class InternalApiKeyFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Internal-Key";
    static final String ROLE = "ROLE_INTERNAL";

    private final byte[] expectedKey;

    InternalApiKeyFilter(String apiKey) {
        this.expectedKey = apiKey == null || apiKey.isBlank() ? null : apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (expectedKey != null && provided != null
                && MessageDigest.isEqual(expectedKey, provided.getBytes(StandardCharsets.UTF_8))) {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "internal-service", null, List.of(new SimpleGrantedAuthority(ROLE))));
        }
        chain.doFilter(request, response);
    }
}
