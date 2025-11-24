package com.justiconsulta.store.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {
    private static final String API_KEY_HEADER = "X-API-KEY";

    @Value("${security.api.secret-key}")
    private String secretKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Solo proteger el endpoint de creación de usuario
        if (request.getRequestURI().equals("/api/users") && request.getMethod().equalsIgnoreCase("POST")) {
            String apiKey = request.getHeader(API_KEY_HEADER);

            // Si la clave no está configurada, rechazar por seguridad
            if (secretKey == null || secretKey.isBlank()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Unauthorized: Server API Key not configured");
                return;
            }

            if (!Objects.equals(secretKey, apiKey)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Unauthorized: Invalid API Key");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}

