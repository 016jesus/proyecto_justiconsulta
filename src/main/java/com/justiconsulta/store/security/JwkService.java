package com.justiconsulta.store.security;

import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URL;
import java.time.Instant;

@Component
public class JwkService {

    private final String jwksUrl;
    private final long ttlSeconds;

    private volatile JWKSet jwkSet;
    private volatile Instant fetchedAt;

    public JwkService(@Value("${supabase.url}") String supabaseUrl,
                      @Value("${security.jwks.ttl:3600}") long ttlSeconds) {
        // supabaseUrl expected like https://<project>.supabase.co
        this.jwksUrl = supabaseUrl.endsWith("/") ? supabaseUrl + "auth/v1/.well-known/jwks.json" : supabaseUrl + "/auth/v1/.well-known/jwks.json";
        this.ttlSeconds = ttlSeconds;
    }

    public synchronized JWKSet getJwkSet(boolean forceRefresh) throws Exception {
        if (!forceRefresh && jwkSet != null && fetchedAt != null) {
            if (Instant.now().isBefore(fetchedAt.plusSeconds(ttlSeconds))) {
                return jwkSet;
            }
        }
        // fetch
        try (InputStream is = new URL(jwksUrl).openStream()) {
            JWKSet newSet = JWKSet.load(is);
            this.jwkSet = newSet;
            this.fetchedAt = Instant.now();
            return this.jwkSet;
        }
    }

    public JWKSet getJwkSet() throws Exception {
        return getJwkSet(false);
    }

    public void forceRefresh() throws Exception {
        getJwkSet(true);
    }
}

