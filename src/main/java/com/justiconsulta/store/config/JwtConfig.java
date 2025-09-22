package com.justiconsulta.store.config;

import com.justiconsulta.store.security.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Bean
    public JwtTokenService jwtTokenService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.ttl-seconds}") long ttlSeconds
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("security.jwt.secret must be defined and not blank");
        }
        return new JwtTokenService(secret, ttlSeconds);
    }
}
