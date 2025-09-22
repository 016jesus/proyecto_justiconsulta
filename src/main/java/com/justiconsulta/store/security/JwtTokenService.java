package com.justiconsulta.store.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;


@Service
public class JwtTokenService {

    private final SecretKey key;
    private final long ttlSeconds;

    public JwtTokenService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.ttl-seconds}") long ttlSeconds
    ) {
        if(secret == null || secret.isBlank()) throw new IllegalArgumentException("JWT secret must be defined and not blank");
        else System.out.println("JWT secret defined: " + secret);
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = ttlSeconds;
    }

    public String generate(String subject, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
    }
}
