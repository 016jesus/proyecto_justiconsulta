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

    public ResponseEntity<String> getByNumeroRadicacion(String numeroRadicacion, Map<String, String> queryParams) {
        String url = baseUrl + ENDPOINT_NUMERO_RADICACION.replace("{numeroRadicacion}", numeroRadicacion);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        if (queryParams != null) {
            queryParams.forEach(builder::queryParam);
        }

        return safeGet(builder.toUriString());
    }

    public ResponseEntity<String> get(String endpoint, Map<String, ?> params) {
        String url = baseUrl + endpoint;
        return safeGet(url);
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
}
