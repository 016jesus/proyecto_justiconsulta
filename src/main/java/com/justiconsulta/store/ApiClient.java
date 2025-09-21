package com.justiconsulta.store;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import org.springframework.http.ResponseEntity;


public class ApiClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;


    public ApiClient(RestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public ResponseEntity<String> get(String endpoint, Map<String, ?> params) {
        String url = baseUrl + endpoint;
        return restTemplate.getForEntity(url, String.class, params);
    }
}
