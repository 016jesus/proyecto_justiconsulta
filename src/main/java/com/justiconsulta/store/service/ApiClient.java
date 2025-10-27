package com.justiconsulta.store.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.util.Objects;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Optional;


@Service
public class ApiClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;


    public static final String ENDPOINT_NUMERO_RADICACION = "/Procesos/Consulta/NumeroRadicacion?numero={numeroRadicacion}";
    public static final String ENDPOINT_PROCESS_DETAIL = "/Proceso/Detalle/{idProceso}";
    public static final String ENDPOINT_PROCESS_SUBJECTS = "/Proceso/Sujetos/{idProceso}";
    public static final String ENDPOINT_PROCESS_DOCUMENTS = "/Proceso/Documentos/{idProceso}";
    public static final String ENDPOINT_PROCESS_ACTUACIONES = "/Proceso/Actuaciones/{idProceso}";

    public ApiClient(RestTemplate restTemplate, @Value("${api.external.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    // Helper that executes GET and converts RestTemplate exceptions into ResponseEntity
    private ResponseEntity<String> safeGet(String uri) {
        try {
            return restTemplate.getForEntity(uri, String.class);
        } catch (HttpClientErrorException e) {
            // propagate client errors (404, 400, etc.) with body
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            // server error from external API
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            // network/connectivity/timeouts
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("External API unreachable: " + e.getMessage());
        } catch (RestClientException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Error calling external API: " + e.getMessage());
        }
    }

    // Validate that numeroRadicacion is exactly 23 digits (trimmed)
    private boolean isValidNumeroRadicacion(String numeroRadicacion) {
        if (numeroRadicacion == null) return false;
        String s = numeroRadicacion.trim();
        return s.matches("^\\d{23}$");
    }

    public ResponseEntity<String> getByNumeroRadicacion(String numeroRadicacion, Map<String, String> queryParams) {
        if (!isValidNumeroRadicacion(numeroRadicacion)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid numeroRadicacion: must be exactly 23 digits");
        }

        String trimmed = numeroRadicacion.trim();
        String url = baseUrl + ENDPOINT_NUMERO_RADICACION.replace("{numeroRadicacion}", trimmed);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        if (queryParams != null) {
            queryParams.forEach(builder::queryParam);
        }

        return safeGet(builder.toUriString());
    }

    public ResponseEntity<String> get(String endpoint, Map<String, ?> params) {
        String url = baseUrl + endpoint;
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        if (params != null && !params.isEmpty()) {
            params.forEach((k, v) -> {
                if (v != null) builder.queryParam(k, v.toString());
            });
        }
        return safeGet(builder.toUriString());
    }

    // New: call /Proceso/Detalle/{idProceso}
    public ResponseEntity<String> getProcessDetail(String idProceso) {
        if (idProceso == null || idProceso.isBlank()) {
            throw new IllegalArgumentException("idProceso is required");
        }
        String url = baseUrl + ENDPOINT_PROCESS_DETAIL.replace("{idProceso}", idProceso);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        return safeGet(builder.toUriString());
    }

    // New: call /Proceso/Sujetos/{idProceso}?pagina={pagina}
    public ResponseEntity<String> getProcessSubjects(String idProceso, int pagina) {
        if (idProceso == null || idProceso.isBlank()) {
            throw new IllegalArgumentException("idProceso is required");
        }
        String url = baseUrl + ENDPOINT_PROCESS_SUBJECTS.replace("{idProceso}", idProceso);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url).queryParam("pagina", pagina);
        return safeGet(builder.toUriString());
    }

    // New: call /Proceso/Documentos/{idProceso}
    public ResponseEntity<String> getProcessDocuments(String idProceso) {
        if (idProceso == null || idProceso.isBlank()) {
            throw new IllegalArgumentException("idProceso is required");
        }
        String url = baseUrl + ENDPOINT_PROCESS_DOCUMENTS.replace("{idProceso}", idProceso);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        return safeGet(builder.toUriString());
    }

    // New: call /Proceso/Actuaciones/{idProceso}?pagina={pagina}
    public ResponseEntity<String> getProcessActuaciones(String idProceso, int pagina) {
        if (idProceso == null || idProceso.isBlank()) {
            throw new IllegalArgumentException("idProceso is required");
        }
        String url = baseUrl + ENDPOINT_PROCESS_ACTUACIONES.replace("{idProceso}", idProceso);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url).queryParam("pagina", pagina);
        return safeGet(builder.toUriString());
    }

    // Validar número de radicación consultando la API remota
    public boolean validateId(String numeroRadicacion) {
        if (numeroRadicacion == null || numeroRadicacion.isBlank()) {
            return false;
        }
        if (!isValidNumeroRadicacion(numeroRadicacion)) {
            return false;
        }
        try {
            ResponseEntity<String> response = getByNumeroRadicacion(numeroRadicacion, Map.of());
            return response != null
                    && response.getStatusCode().is2xxSuccessful()
                    && Objects.nonNull(response.getBody())
                    && !response.getBody().isBlank();
        } catch (Exception e) {
            return false;
        }
    }

    // New: Try to extract the last action date from the NumeroRadicacion response body
    public Optional<OffsetDateTime> getLastActionDateByNumeroRadicacion(String numeroRadicacion) {
        ResponseEntity<String> resp = getByNumeroRadicacion(numeroRadicacion, Map.of());
        if (resp == null || !resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null || resp.getBody().isBlank()) {
            return Optional.empty();
        }
        String body = resp.getBody();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(body);
            // Heurísticas: buscar campos comunes
            // 1) campo directo 'fechaUltimaActuacion' o 'lastActionDate'
            String[] directKeys = new String[]{"fechaUltimaActuacion", "lastActionDate", "ultimaActuacionFecha"};
            for (String k : directKeys) {
                JsonNode n = root.get(k);
                if (n != null && n.isTextual()) {
                    try { return Optional.of(OffsetDateTime.parse(n.asText())); } catch (Exception ignore) {}
                }
            }
            // 2) objeto 'ultimaActuacion' con campo 'fechaActuacion'
            JsonNode ua = root.get("ultimaActuacion");
            if (ua != null && ua.isObject()) {
                JsonNode f = ua.get("fechaActuacion");
                if (f != null && f.isTextual()) {
                    try { return Optional.of(OffsetDateTime.parse(f.asText())); } catch (Exception ignore) {}
                }
            }
            // 3) si es arreglo, tomar el primer elemento que tenga 'fechaActuacion'
            if (root.isArray() && root.size() > 0) {
                JsonNode first = root.get(0);
                if (first != null) {
                    JsonNode f = first.get("fechaActuacion");
                    if (f != null && f.isTextual()) {
                        try { return Optional.of(OffsetDateTime.parse(f.asText())); } catch (Exception ignore) {}
                    }
                }
            }
        } catch (Exception e) {
            // ignore parse errors; return empty
        }
        return Optional.empty();
    }
}
