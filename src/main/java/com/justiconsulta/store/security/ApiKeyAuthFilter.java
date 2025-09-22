package com.justiconsulta.store.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {
    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final String SECRET_KEY = "APP-CLIENT-a20ceb8b-b6c3-4620-a560-45c39746a30c"; // Cambia esto por tu clave real

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Solo proteger el endpoint de creación de usuario
        if (request.getRequestURI().equals("/api/users") && request.getMethod().equalsIgnoreCase("POST")) {
            String apiKey = request.getHeader(API_KEY_HEADER);
            if (!SECRET_KEY.equals(apiKey)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Unauthorized: Invalid API Key");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}

