package com.justiconsulta.store.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupabaseClient {

    private static final Logger log = LoggerFactory.getLogger(SupabaseClient.class);

    private final RestTemplate restTemplate;

    // Support either 'supabase.url' (preferred) or environment-like 'SUPABASE_URL'
    @Value("${supabase.url:${SUPABASE_URL:}}")
    private String supabaseUrl;

    // Support either 'supabase.service-role-key' or 'SUPABASE_SERVICE_ROLE_KEY'
    @Value("${supabase.service-role-key:${SUPABASE_SERVICE_ROLE_KEY:}}")
    private String serviceRoleKey;

    @PostConstruct
    public void init() {
        if (supabaseUrl == null || supabaseUrl.isEmpty()) {
            log.warn("Supabase URL is not configured (property 'supabase.url' or env 'SUPABASE_URL'). Supabase operations will fail.");
        }
        if (serviceRoleKey == null || serviceRoleKey.isEmpty()) {
            log.warn("Supabase service role key is not configured (property 'supabase.service-role-key' or env 'SUPABASE_SERVICE_ROLE_KEY'). Supabase operations will fail.");
        }
    }

    // Create user via Supabase Admin API and return the supabase user id as UUID
    public UUID createUser(String email, String password, Map<String, Object> userMetadata) {
        if (supabaseUrl == null || supabaseUrl.isEmpty() || serviceRoleKey == null || serviceRoleKey.isEmpty()) {
            throw new IllegalStateException("Supabase is not configured properly. Missing URL or service role key.");
        }

        String url = supabaseUrl;
        // ensure base url does not end with slash
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        url = url + "/auth/v1/admin/users";

        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        if (userMetadata != null) body.put("user_metadata", userMetadata);
        body.put("email_confirm", true);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(serviceRoleKey);
        headers.add("apikey", serviceRoleKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(url, HttpMethod.POST, request, (Class<Map<String, Object>>)(Class)Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                Object id = resp.getBody().get("id");
                if (id != null) {
                    try {
                        return UUID.fromString(id.toString());
                    } catch (IllegalArgumentException ex) {
                        throw new RuntimeException("Supabase returned invalid UUID for id: " + id.toString(), ex);
                    }
                }
            }
            throw new RuntimeException("Failed to create user in Supabase: " + resp.getStatusCode());
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Supabase API error: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
        }
    }

    // New: trigger password recovery email via Supabase public recover endpoint
    public void sendPasswordRecovery(String email) {
        if (supabaseUrl == null || supabaseUrl.isEmpty()) {
            throw new IllegalStateException("Supabase URL is not configured. Cannot send recovery email.");
        }

        String url = supabaseUrl;
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        url = url + "/auth/v1/recover";

        Map<String, Object> body = new HashMap<>();
        body.put("email", email);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // for recover endpoint Supabase expects an apikey header (anon or service role). We'll add serviceRoleKey if available
        if (serviceRoleKey != null && !serviceRoleKey.isEmpty()) {
            headers.add("apikey", serviceRoleKey);
            headers.setBearerAuth(serviceRoleKey);
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, request, Map.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Supabase recover request failed: " + resp.getStatusCode());
            }
            log.info("Supabase recover request accepted for email={}", email);
        } catch (HttpClientErrorException e) {
            // If Supabase returns 400/404, surface a clear message
            throw new RuntimeException("Supabase recover API error: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
        }
    }

    // Update current user password using an access token (from recovery link) via Supabase Auth API
    public void updatePasswordWithAccessToken(String accessToken, String newPassword) {
        if (supabaseUrl == null || supabaseUrl.isEmpty()) {
            throw new IllegalStateException("Supabase URL is not configured. Cannot update password.");
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Missing access token");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        String url = supabaseUrl;
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        url = url + "/auth/v1/user";

        Map<String, Object> body = new HashMap<>();
        body.put("password", newPassword);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        // apikey header is generally required; use service role if available
        if (serviceRoleKey != null && !serviceRoleKey.isEmpty()) {
            headers.add("apikey", serviceRoleKey);
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            // GoTrue typically accepts PUT/PATCH; use PUT here
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.PUT, request, Map.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Supabase update password failed: " + resp.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            // 401 when token invalid/expired; 400 for malformed
            throw new IllegalArgumentException("Access token inválido o expirado: " + e.getStatusCode());
        }
    }
}
