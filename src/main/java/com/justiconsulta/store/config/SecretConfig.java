package com.justiconsulta.store.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class SecretConfig {
  @Value("${security.api.secret-key}")
  private String apiSecretKey;

  // Supabase JWT secret (backend only)
  @Value("${security.supabase.jwt-secret:}")
  private String supabaseJwtSecret;

  // Expected issuer, e.g., https://<project-ref>.supabase.co/auth/v1
  @Value("${security.supabase.issuer:}")
  private String supabaseIssuer;
}
