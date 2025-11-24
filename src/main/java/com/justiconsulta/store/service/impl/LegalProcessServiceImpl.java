package com.justiconsulta.store.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.justiconsulta.store.dto.response.HistoryResponseDto;
import com.justiconsulta.store.dto.response.LegalProcessResponseDto;
import com.justiconsulta.store.model.History;
import com.justiconsulta.store.model.LegalProcess;
import com.justiconsulta.store.model.User;
import com.justiconsulta.store.model.UserLegalProcess;
import com.justiconsulta.store.model.UserLegalProcess.UserLegalProcessId;
import com.justiconsulta.store.repository.HistoryRepository;
import com.justiconsulta.store.repository.LegalProcessRepository;
import com.justiconsulta.store.repository.UserLegalProcessRepository;
import com.justiconsulta.store.repository.UserRepository;
import com.justiconsulta.store.service.ApiClient;
import com.justiconsulta.store.service.contract.ILegalProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LegalProcessServiceImpl implements ILegalProcessService {
    private static final Logger log = LoggerFactory.getLogger(LegalProcessServiceImpl.class);

    private final LegalProcessRepository legalProcessRepository;
    private final ApiClient apiClient;
    private final UserRepository userRepository;
    private final UserLegalProcessRepository userLegalProcessRepository;
    private final HistoryRepository historyRepository;

    public LegalProcessServiceImpl(LegalProcessRepository legalProcessRepository, ApiClient apiClient,
                                   UserRepository userRepository, UserLegalProcessRepository userLegalProcessRepository,
                                   HistoryRepository historyRepository) {
        this.legalProcessRepository = legalProcessRepository;
        this.apiClient = apiClient;
        this.userRepository = userRepository;
        this.userLegalProcessRepository = userLegalProcessRepository;
        this.historyRepository = historyRepository;
    }

    @Override
    public ResponseEntity<?> removeAssociation(String numeroRadicacion) {
        if (!isValidNumeroRadicacion(numeroRadicacion)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "El número de radicación debe tener exactamente 23 dígitos numéricos"));
        }

        Optional<String> documentNumberOpt = resolveDocumentNumberFromAuth();
        if (documentNumberOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "No autenticado o no se pudo resolver el usuario desde el token"));
        }
        String documentNumber = documentNumberOpt.get();

        Optional<User> userOpt = userRepository.findByDocumentNumber(documentNumber);
        if (userOpt.isEmpty()) {
            return ResponseEntity.unprocessableEntity().body(Map.of("message", "Usuario no encontrado."));
        }

        UserLegalProcessId id = new UserLegalProcessId(documentNumber, numeroRadicacion);
        if (!userLegalProcessRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "La asociación no existe."));
        }

        try {
            userLegalProcessRepository.deleteById(id);
        } catch (Exception e) {
            log.warn("Error al eliminar la asociación para usuario {} y proceso {}: {}", documentNumber, numeroRadicacion, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "No fue posible eliminar la asociación."));
        }

        return ResponseEntity.ok(Map.of("message", "Asociación eliminada correctamente."));
    }

    @Override
    public ResponseEntity<List<LegalProcessResponseDto>> getAllLegalProcesses() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String principal = auth.getName();
        String resolvedDocumentNumber = resolveDocumentNumber(principal);

        if (resolvedDocumentNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<User> user = userRepository.findByDocumentNumber(resolvedDocumentNumber);
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<LegalProcess> processes = legalProcessRepository.findByIdUserDocumentNumber(resolvedDocumentNumber);
        List<LegalProcessResponseDto> dtos = processes.stream()
                .map(p -> new LegalProcessResponseDto(
                        p.getId() != null ? p.getId().getId() : null,
                        p.getLastActionDate(),
                        p.getCreatedAt()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Override
    public ResponseEntity<?> associateProcessToUser(String numeroRadicacion) {
        if (!isValidNumeroRadicacion(numeroRadicacion)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "El número de radicación debe tener exactamente 23 dígitos numéricos"));
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autenticado");
        }

        String principal = auth.getName();
        String documentNumber = resolveDocumentNumber(principal);

        if (documentNumber == null || documentNumber.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("No fue posible resolver el usuario desde el token");
        }

        Optional<User> user = userRepository.findByDocumentNumber(documentNumber);
        if (user.isEmpty()) {
            return ResponseEntity.unprocessableEntity().body("Usuario no encontrado.");
        }

        boolean processExists = apiClient.validateId(numeroRadicacion);
        if (!processExists) {
            return ResponseEntity.unprocessableEntity().body("Proceso no encontrado.");
        }

        if (!ensureLegalProcessExists(numeroRadicacion, documentNumber)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("No fue posible crear el proceso legal en BD");
        }

        UserLegalProcessId id = new UserLegalProcessId(documentNumber, numeroRadicacion);
        if (userLegalProcessRepository.existsById(id)) {
            return ResponseEntity.unprocessableEntity().body("La asociación ya existe.");
        }

        UserLegalProcess association = new UserLegalProcess(id, OffsetDateTime.now());
        userLegalProcessRepository.save(association);
        return ResponseEntity.ok("Asociación creada correctamente.");
    }

    @Override
    public ResponseEntity<List<HistoryResponseDto>> getUserHistory() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String principal = auth.getName();
        String resolvedDocumentNumber = resolveDocumentNumber(principal);

        if (resolvedDocumentNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<User> user = userRepository.findByDocumentNumber(resolvedDocumentNumber);
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<History> history = historyRepository.findByUserDocumentNumberOrderByDateDesc(resolvedDocumentNumber);
        List<HistoryResponseDto> dtos = history.stream()
                .map(h -> new HistoryResponseDto(h.getId(), h.getLegalProcessId(), h.getActivitySeriesId(),
                        h.getDate(), h.getResult(), h.getCreatedAt(), h.getUserDocumentNumber()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Override
    public ResponseEntity<?> getLegalProcess(String numeroRadicacion, boolean soloActivos, int pagina, String documentNumberHeader) {
        if (!isValidNumeroRadicacion(numeroRadicacion)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "El número de radicación debe tener exactamente 23 dígitos numéricos"));
        }

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("SoloActivos", String.valueOf(soloActivos));
        queryParams.put("pagina", String.valueOf(pagina));

        ResponseEntity<String> response = apiClient.getByNumeroRadicacion(numeroRadicacion, queryParams);

        String resolvedDocumentNumber = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            String principal = auth.getName();
            resolvedDocumentNumber = resolveDocumentNumber(principal);
        }

        if (resolvedDocumentNumber == null && documentNumberHeader != null && !documentNumberHeader.isBlank()) {
            resolvedDocumentNumber = documentNumberHeader;
        }

        if (resolvedDocumentNumber != null) {
            Optional<User> userOpt = userRepository.findByDocumentNumber(resolvedDocumentNumber);
            if (userOpt.isPresent()) {
                saveHistory(resolvedDocumentNumber, numeroRadicacion, response);
            }
        }

        if (response == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "No response from external API"));
        }
        if (response.getStatusCode().is2xxSuccessful()) {
            if (response.getBody() != null) {
                return ResponseEntity.ok(response.getBody());
            }
            return ResponseEntity.ok(Map.of("message", "No content from external API"));
        }
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @Override
    public ResponseEntity<?> publicGetLegalProcess(String numeroRadicacion, boolean soloActivos, int pagina) {
        if (!isValidNumeroRadicacion(numeroRadicacion)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "El número de radicación debe tener exactamente 23 dígitos numéricos"));
        }
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("SoloActivos", String.valueOf(soloActivos));
        queryParams.put("pagina", String.valueOf(pagina));
        ResponseEntity<String> response = apiClient.getByNumeroRadicacion(numeroRadicacion, queryParams);

        if (response == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "No response from external API"));
        }
        if (response.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.ok(response.getBody() != null ? response.getBody() :
                    Map.of("message", "No content from external API"));
        }
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @Override
    public ResponseEntity<?> getProcessDetail(String idProceso) {
        String resolvedId;
        if (isValidNumeroRadicacion(idProceso)) {
            Optional<String> idOpt = apiClient.getProcessIdByNumeroRadicacion(idProceso);
            if (idOpt.isEmpty() || idOpt.get().isBlank()) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(Map.of("message", "Proceso no encontrado."));
            }
            resolvedId = idOpt.get();
        } else if (isNumeric(idProceso)) {
            resolvedId = idProceso;
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Parámetro inválido: debe ser número de radicación (23 dígitos) o idProceso numérico"));
        }

        ResponseEntity<String> response = apiClient.getProcessDetail(resolvedId);

        if (response == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "No response from external API"));
        }
        if (response.getStatusCode().is2xxSuccessful()) {
            if (response.getBody() != null) return ResponseEntity.ok(response.getBody());
            return ResponseEntity.ok(Map.of("message", "No content from external API"));
        }
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @Override
    public ResponseEntity<?> getProcessSubjects(String idProceso, int pagina) {
        String resolvedId;
        if (isValidNumeroRadicacion(idProceso)) {
            Optional<String> idOpt = apiClient.getProcessIdByNumeroRadicacion(idProceso);
            if (idOpt.isEmpty() || idOpt.get().isBlank()) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(Map.of("message", "Proceso no encontrado."));
            }
            resolvedId = idOpt.get();
        } else if (isNumeric(idProceso)) {
            resolvedId = idProceso;
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Parámetro inválido: debe ser número de radicación (23 dígitos) o idProceso numérico"));
        }

        ResponseEntity<String> response = apiClient.getProcessSubjects(resolvedId, pagina);

        if (response == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "No response from external API"));
        }
        if (response.getStatusCode().is2xxSuccessful()) {
            if (response.getBody() != null && !response.getBody().isBlank())
                return ResponseEntity.ok(response.getBody());
            return ResponseEntity.ok(Map.of("message", "No subjects associated with this process"));
        }
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @Override
    public ResponseEntity<?> getProcessDocuments(String idProceso) {
        String resolvedId;
        if (isValidNumeroRadicacion(idProceso)) {
            Optional<String> idOpt = apiClient.getProcessIdByNumeroRadicacion(idProceso);
            if (idOpt.isEmpty() || idOpt.get().isBlank()) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(Map.of("message", "Proceso no encontrado."));
            }
            resolvedId = idOpt.get();
        } else if (isNumeric(idProceso)) {
            resolvedId = idProceso;
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Parámetro inválido: debe ser número de radicación (23 dígitos) o idProceso numérico"));
        }

        ResponseEntity<String> response = apiClient.getProcessDocuments(resolvedId);

        if (response == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "No response from external API"));
        }
        if (response.getStatusCode().is2xxSuccessful()) {
            if (response.getBody() != null && !response.getBody().isBlank())
                return ResponseEntity.ok(response.getBody());
            return ResponseEntity.ok(Map.of("message", "El proceso no tiene documentos asociados"));
        }
        // Si es 404, no es error, simplemente no hay documentos
        if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
            return ResponseEntity.ok(Map.of("message", "El proceso no tiene documentos asociados"));
        }
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @Override
    public ResponseEntity<?> getProcessActuaciones(String idProceso, int pagina) {
        String resolvedId;
        if (isValidNumeroRadicacion(idProceso)) {
            Optional<String> idOpt = apiClient.getProcessIdByNumeroRadicacion(idProceso);
            if (idOpt.isEmpty() || idOpt.get().isBlank()) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(Map.of("message", "Proceso no encontrado."));
            }
            resolvedId = idOpt.get();
        } else if (isNumeric(idProceso)) {
            resolvedId = idProceso;
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Parámetro inválido: debe ser número de radicación (23 dígitos) o idProceso numérico"));
        }

        ResponseEntity<String> response = apiClient.getProcessActuaciones(resolvedId, pagina);

        if (response == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "No response from external API"));
        }
        if (response.getStatusCode().is2xxSuccessful()) {
            if (response.getBody() != null && !response.getBody().isBlank())
                return ResponseEntity.ok(response.getBody());
            return ResponseEntity.ok(Map.of("message", "No actuaciones asociadas a este proceso"));
        }
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    // Helper methods
    private boolean isValidNumeroRadicacion(String numeroRadicacion) {
        return numeroRadicacion != null && numeroRadicacion.matches("\\d{23}");
    }

    private boolean isNumeric(String value) {
        return value != null && value.matches("\\d+");
    }

    private Optional<String> resolveDocumentNumberFromAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return Optional.empty();
        }
        String principal = auth.getName();
        return Optional.ofNullable(resolveDocumentNumber(principal));
    }

    private String resolveDocumentNumber(String principal) {
        if (principal == null || principal.isBlank()) return null;
        try {
            UUID supabaseId = UUID.fromString(principal);
            Optional<User> u = userRepository.findBySupabaseUserId(supabaseId);
            if (u.isPresent()) return u.get().getDocumentNumber();
        } catch (IllegalArgumentException ex) {
            Optional<User> u2 = userRepository.findByEmail(principal);
            if (u2.isPresent()) return u2.get().getDocumentNumber();
        }
        return null;
    }

    private boolean ensureLegalProcessExists(String numeroRadicacion, String documentNumber) {
        LegalProcess.LegalProcessId lpId = new LegalProcess.LegalProcessId(numeroRadicacion, documentNumber);
        Optional<LegalProcess> existing = legalProcessRepository.findById(lpId);
        if (existing.isPresent()) return true;

        Optional<OffsetDateTime> lastActionOpt = apiClient.getLastActionDateByNumeroRadicacion(numeroRadicacion);

        LegalProcess lp = new LegalProcess();
        lp.setId(lpId);
        lp.setLastActionDate(lastActionOpt.orElse(null));
        lp.setCreatedAt(OffsetDateTime.now());
        try {
            legalProcessRepository.save(lp);
            return true;
        } catch (Exception e) {
            return legalProcessRepository.findById(lpId).isPresent();
        }
    }

    private void saveHistory(String documentNumber, String numeroRadicacion, ResponseEntity<String> response) {
        History history = new History();
        history.setUserDocumentNumber(documentNumber);
        String idParaHistorial = extractIdProcesoFromResponse(numeroRadicacion, response);
        history.setLegalProcessId(idParaHistorial);
        history.setActivitySeriesId(null);
        history.setDate(OffsetDateTime.now());
        if (response != null && response.getBody() != null) {
            String body = response.getBody();
            history.setResult(body.length() > 2000 ? body.substring(0, 2000) : body);
        } else if (response != null) {
            history.setResult("HTTP " + response.getStatusCode().value());
        } else {
            history.setResult("No response from external API");
        }
        history.setCreatedAt(OffsetDateTime.now());
        try {
            historyRepository.save(history);
        } catch (Exception e) {
            log.warn("Failed to save history for user {} and process {}: {}", documentNumber, numeroRadicacion, e.getMessage());
        }
    }

    private String extractIdProcesoFromResponse(String numeroRadicacion, ResponseEntity<String> response) {
        if (response == null || response.getBody() == null) return numeroRadicacion;
        String body = response.getBody();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(body);
            List<String> candidates = new ArrayList<>();
            if (root.isObject()) {
                if (root.has("idProceso")) candidates.add(root.get("idProceso").asText());
                if (root.has("id")) candidates.add(root.get("id").asText());
                if (root.has("id_proceso")) candidates.add(root.get("id_proceso").asText());
            }
            if (root.isArray() && !root.isEmpty()) {
                JsonNode first = root.get(0);
                if (first.has("idProceso")) candidates.add(first.get("idProceso").asText());
                if (first.has("id")) candidates.add(first.get("id").asText());
                if (first.has("id_proceso")) candidates.add(first.get("id_proceso").asText());
            }
            if (candidates.isEmpty()) {
                Iterator<String> it = root.fieldNames();
                while (it.hasNext()) {
                    String name = it.next();
                    JsonNode val = root.get(name);
                    String v = val != null ? val.asText() : null;
                    if (v != null && v.matches("\\d{3,12}")) {
                        candidates.add(v);
                    }
                }
            }
            for (String c : candidates) {
                if (c != null && !c.isBlank()) {
                    if (c.length() < numeroRadicacion.length()) return c;
                    return c;
                }
            }
        } catch (Exception e) {
            log.debug("No se pudo parsear body para extraer idProceso: {}", e.getMessage());
        }
        log.warn("No se pudo extraer idProceso desde la respuesta externa para numeroRadicacion={}, se usará el numero completo en historial", numeroRadicacion);
        return numeroRadicacion;
    }

    /**
     * Método auxiliar para obtener información del despacho desde la API
     */
    private String getDespachoFromApi(String numeroRadicacion) {
        try {
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("SoloActivos", "false");
            queryParams.put("pagina", "1");
            ResponseEntity<String> response = apiClient.getByNumeroRadicacion(numeroRadicacion, queryParams);

            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.getBody());

                // Buscar el campo "despacho" en la respuesta
                if (root.has("procesos") && root.get("procesos").isArray() && !root.get("procesos").isEmpty()) {
                    JsonNode firstProcess = root.get("procesos").get(0);
                    if (firstProcess.has("despacho")) {
                        return firstProcess.get("despacho").asText();
                    }
                }

                // Alternativa: buscar directamente en el objeto raíz
                if (root.has("despacho")) {
                    return root.get("despacho").asText();
                }
            }
        } catch (Exception e) {
            log.debug("No se pudo obtener información del despacho para el proceso {}: {}", numeroRadicacion, e.getMessage());
        }

        return "Información no disponible";
    }
}
