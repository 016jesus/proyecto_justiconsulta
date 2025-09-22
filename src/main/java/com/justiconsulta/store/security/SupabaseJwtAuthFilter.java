package com.justiconsulta.store.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.justiconsulta.store.config.SecretConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class SupabaseJwtAuthFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretConfig secretConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SupabaseJwtAuthFilter(SecretConfig secretConfig) {
        this.secretConfig = secretConfig;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(AUTH_HEADER);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();

        try {
            String jwtSecret = secretConfig.getSupabaseJwtSecret();
            if (jwtSecret == null || jwtSecret.isBlank()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Unauthorized: Supabase JWT secret not configured");
                return;
            }

            // Parse token
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                unauthorized(response, "Invalid token format");
                return;
            }
            String headerPart = parts[0];
            String payloadPart = parts[1];
            String signaturePart = parts[2];

            // Verify signature HS256
            if (!verifyHs256Signature(headerPart, payloadPart, signaturePart, jwtSecret)) {
                unauthorized(response, "Invalid token signature");
                return;
            }

            // Decode payload
            byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadPart);
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(payloadBytes, Map.class);

            // Validate exp
            Object expObj = claims.get("exp");
            long now = Instant.now().getEpochSecond();
            long exp = (expObj instanceof Number) ? ((Number) expObj).longValue() : 0L;
            if (exp <= now) {
                unauthorized(response, "Token expired");
                return;
            }

            // Validate issuer if configured
            String expectedIss = secretConfig.getSupabaseIssuer();
            String iss = (String) claims.get("iss");
            if (expectedIss != null && !expectedIss.isBlank() && !Objects.equals(expectedIss, iss)) {
                unauthorized(response, "Invalid token issuer");
                return;
            }

            // Build Authentication
            String subject = (String) claims.get("sub"); // UUID del usuario en Supabase
            String email = (String) claims.get("email");

            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            subject != null ? subject : email,
                            null,
                            authorities
                    );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            unauthorized(response, "Unauthorized: " + ex.getMessage());
        }
    }

    private boolean verifyHs256Signature(String headerPart, String payloadPart, String signaturePart, String secret) throws Exception {
        String signingInput = headerPart + "." + payloadPart;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] computed = mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII));
        String computedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(computed);
        return computedSignature.equals(signaturePart);
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(message);
    }
}
