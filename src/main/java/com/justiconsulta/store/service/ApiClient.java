package com.justiconsulta.store.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ApiClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;


    public static final String ENDPOINT_NUMERO_RADICACION = "/Procesos/Consulta/NumeroRadicacion?numero={numeroRadicacion}";

    public ApiClient(RestTemplate restTemplate, @Value("${api.external.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }


    public ResponseEntity<String> getByNumeroRadicacion(String numeroRadicacion, Map<String, String> queryParams) {
        String url = baseUrl + ENDPOINT_NUMERO_RADICACION.replace("{numeroRadicacion}", numeroRadicacion);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        if (queryParams != null) {
            queryParams.forEach(builder::queryParam);
        }

        return restTemplate.getForEntity(builder.toUriString(), String.class);
    }

    // Metodo genérico para otros endpoints
    public ResponseEntity<String> get(String endpoint, Map<String, ?> params) {
        String url = baseUrl + endpoint;
        return restTemplate.getForEntity(url, String.class, params);
    }

    // Aquí puedes agregar más métodos para otros endpoints en el futuro
}
