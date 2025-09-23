package com.justiconsulta.store.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

// Quitar @Service; se declara vía @Bean en JwtConfig
public class JwtTokenService {

    private final SecretKey secretKey;
    private final long ttlSeconds;

    // Constructor esperado por JwtConfig
    public JwtTokenService(String secret, long ttlSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = ttlSeconds;
    }

    private SecretKey signingKey() {
        return secretKey;
    }

    // Valida el JWT Bearer y construye un principal de Spring Security
    public org.springframework.security.core.userdetails.User validateAndGetPrincipal(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        String username = claims.getSubject();
        if (username == null || username.isBlank()) {
            return null;
        }

        Object rolesClaim = claims.get("roles");
        Collection<SimpleGrantedAuthority> authorities;
        if (rolesClaim instanceof List<?> list) {
            authorities = list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        } else {
            authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return new User(username, "", authorities);
    }

    // Genera un JWT con HS256, sujeto = documentNumber y expiración configurable (ttlSeconds)
    public String generate(String documentNumber, String email) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);

        return Jwts.builder()
                .setSubject(documentNumber)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .claim("email", email)
                .claim("roles", List.of("ROLE_USER"))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}
