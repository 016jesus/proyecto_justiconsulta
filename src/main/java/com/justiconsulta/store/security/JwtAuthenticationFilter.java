package com.justiconsulta.store.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final TokenValidator tokenValidator;

    public JwtAuthenticationFilter(TokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                TokenValidationResult result = tokenValidator.validate(token);
                if (result != null && result.isValid()) {
                    String principal = result.getPrincipal();
                    if (principal != null && !principal.isBlank()) {
                        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, result.getAuthorities().stream().map(a -> (org.springframework.security.core.authority.SimpleGrantedAuthority) new org.springframework.security.core.authority.SimpleGrantedAuthority(a)).collect(Collectors.toList()));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (Exception ex) {
                logger.warn("Token validation error", ex);
                // If validation throws, do not set Authentication and continue; endpoints protected will return 401
            }
        }
        filterChain.doFilter(request, response);
    }
}

