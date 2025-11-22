package com.justiconsulta.store.controller;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

@RestController
@RequestMapping("/api/legal-processes")
public class LegalProcessController {
    private static final Logger log = LoggerFactory.getLogger(LegalProcessController.class);

    private final LegalProcessRepository legalProcessRepository;
    private final ApiClient apiClient;
    private final UserRepository userRepository;
    private final UserLegalProcessRepository userLegalProcessRepository;
    private final HistoryRepository historyRepository;

    public LegalProcessController(LegalProcessRepository legalProcessRepository, ApiClient apiClient, UserRepository userRepository, UserLegalProcessRepository userLegalProcessRepository, HistoryRepository historyRepository) {
        this.legalProcessRepository = legalProcessRepository;
        this.apiClient = apiClient;
        this.userRepository = userRepository;
        this.userLegalProcessRepository = userLegalProcessRepository;
        this.historyRepository = historyRepository;
    }

    // Helper: valida número de radicación de 23 dígitos
    private boolean isValidNumeroRadicacion(String numeroRadicacion) {
        return numeroRadicacion != null && numeroRadicacion.matches("\\d{23}");
    }

    // Helper: si el valor parece numeroRadicacion (23 dígitos), resolver idProceso consultando la API
    private Optional<String> resolveIdProcesoIfRadicacion(String maybeIdOrRad) {
        if (isValidNumeroRadicacion(maybeIdOrRad)) {
            return apiClient.getProcessIdByNumeroRadicacion(maybeIdOrRad);
        }
        return Optional.ofNullable(maybeIdOrRad);
    }

    // Helper encapsulado: garantiza la existencia de legal_process y fija lastActionDate desde la API
    private boolean ensureLegalProcessExists(String numeroRadicacion, String documentNumber) {
        LegalProcess.LegalProcessId lpId = new LegalProcess.LegalProcessId(numeroRadicacion, documentNumber);
        Optional<LegalProcess> existing = legalProcessRepository.findById(lpId);
        if (existing.isPresent()) return true;

        Optional<java.time.OffsetDateTime> lastActionOpt = apiClient.getLastActionDateByNumeroRadicacion(numeroRadicacion);

        LegalProcess lp = new LegalProcess();
        lp.setId(lpId);
        lp.setLastActionDate(lastActionOpt.orElse(null));
        lp.setCreatedAt(java.time.OffsetDateTime.now());
        try {
            legalProcessRepository.save(lp);
            return true;
        } catch (Exception e) {
            // Posible carrera: si ya existe después del intento, continuar; si no, fallar
            return legalProcessRepository.findById(lpId).isPresent();
        }
    }

    // Helper para resolver el documentNumber desde el Authentication (JWT) o vacío si no se puede
    private Optional<String> resolveDocumentNumberFromAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return Optional.empty();
        }
        String principal = auth.getName();
        if (principal == null || principal.isBlank()) return Optional.empty();
        try {
            UUID supabaseId = UUID.fromString(principal);
            Optional<User> u = userRepository.findBySupabaseUserId(supabaseId);
            if (u.isPresent()) return Optional.ofNullable(u.get().getDocumentNumber());
        } catch (IllegalArgumentException ex) {
            Optional<User> u2 = userRepository.findByEmail(principal);
            if (u2.isPresent()) return Optional.ofNullable(u2.get().getDocumentNumber());
        }
        return Optional.empty();
    }

    // Helper adicional: numérico genérico
    private boolean isNumeric(String value) {
        return value != null && value.matches("\\d+");
    }

    @DeleteMapping("/{numeroRadicacion}")
    public ResponseEntity<?> removeAssociation(
            @PathVariable String numeroRadicacion
    ) {
        // Validación de número de radicación (23 dígitos)
        if (!isValidNumeroRadicacion(numeroRadicacion)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "El número de radicación debe tener exactamente 23 dígitos numéricos"));
        }

        Optional<String> documentNumberOpt = resolveDocumentNumberFromAuth();
        if (documentNumberOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "No autenticado o no se pudo resolver el usuario desde el token"));
        }
        String documentNumber = documentNumberOpt.get();

        Optional<User> userOpt = userRepository.findByDocumentNumber(documentNumber);
        if (userOpt.isEmpty()) {
            return ResponseEntity.unprocessableEntity().body(Map.of("message", "Usuario no encontrado."));
        }

        UserLegalProcess.UserLegalProcessId id = new UserLegalProcessId(documentNumber, numeroRadicacion);
        if (!userLegalProcessRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "La asociación no existe."));
        }

        try {
            userLegalProcessRepository.deleteById(id);
        } catch (Exception e) {
            log.warn("Error al eliminar la asociación para usuario {} y proceso {}: {}", documentNumber, numeroRadicacion, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "No fue posible eliminar la asociación."));
        }

        return ResponseEntity.ok(Map.of("message", "Asociación eliminada correctamente."));
    }

    @GetMapping
    public ResponseEntity<List<LegalProcessResponseDto>> getAllLegalProcesses() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String principal = auth.getName();
        String resolvedDocumentNumber = null;
        if (principal != null && !principal.isBlank()) {
            try {
                UUID supabaseId = UUID.fromString(principal);
                Optional<User> u = userRepository.findBySupabaseUserId(supabaseId);
                if (u.isPresent()) resolvedDocumentNumber = u.get().getDocumentNumber();
            } catch (IllegalArgumentException ex) {
                Optional<User> u2 = userRepository.findByEmail(principal);
                if (u2.isPresent()) resolvedDocumentNumber = u2.get().getDocumentNumber();
            }
        }

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
    // Removed request body; ahora se obtiene el documentNumber desde el JWT
    @PostMapping("/{numeroRadicacion}")
    public ResponseEntity<?> associateProcessToUser(
            @PathVariable String numeroRadicacion
    ) {
        // Validación de número de radicación (23 dígitos)
        if (!isValidNumeroRadicacion(numeroRadicacion)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "El número de radicación debe tener exactamente 23 dígitos numéricos"));
        }

        // Resolver documentNumber desde el JWT (SecurityContext)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autenticado");
        }

        String principal = auth.getName();
        String documentNumber = null;
        if (principal != null && !principal.isBlank()) {
            try {
                UUID supabaseId = UUID.fromString(principal);
                Optional<User> u = userRepository.findBySupabaseUserId(supabaseId);
                if (u.isPresent()) documentNumber = u.get().getDocumentNumber();
            } catch (IllegalArgumentException ex) {
                Optional<User> u2 = userRepository.findByEmail(principal);
                if (u2.isPresent()) documentNumber = u2.get().getDocumentNumber();
            }
        }

        if (documentNumber == null || documentNumber.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No fue posible resolver el usuario desde el token");
        }

        Optional<User> user = userRepository.findByDocumentNumber(documentNumber);
        if (user.isEmpty()) {
            return ResponseEntity.unprocessableEntity().body("Usuario no encontrado.");
        }

        // Validar existencia del proceso llamando a la API externa (no por repositorio local)
        boolean processExists = apiClient.validateId(numeroRadicacion);
        if (!processExists) {
            return ResponseEntity.unprocessableEntity().body("Proceso no encontrado.");
        }

        // Asegurar que exista el registro en legal_process (y setear lastActionDate desde API)
        if (!ensureLegalProcessExists(numeroRadicacion, documentNumber)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("No fue posible crear el proceso legal en BD");
        }

        UserLegalProcessId id = new UserLegalProcessId(documentNumber, numeroRadicacion);
        if (userLegalProcessRepository.existsById(id)) {
            return ResponseEntity.unprocessableEntity().body("La asociación ya existe.");
        }
        // persistir con createdAt actual
        UserLegalProcess association = new UserLegalProcess(id, OffsetDateTime.now());
        userLegalProcessRepository.save(association);
        return ResponseEntity.ok("Asociación creada correctamente.");
    }

    @GetMapping("/history")
    public ResponseEntity<java.util.List<HistoryResponseDto>> getUserHistory() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String principal = auth.getName();
        String resolvedDocumentNumber = null;
        if (principal != null && !principal.isBlank()) {
            try {
                UUID supabaseId = UUID.fromString(principal);
                Optional<User> u = userRepository.findBySupabaseUserId(supabaseId);
                if (u.isPresent()) resolvedDocumentNumber = u.get().getDocumentNumber();
            } catch (IllegalArgumentException ex) {
                Optional<User> u2 = userRepository.findByEmail(principal);
                if (u2.isPresent()) resolvedDocumentNumber = u2.get().getDocumentNumber();
            }
        }

        if (resolvedDocumentNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<User> user = userRepository.findByDocumentNumber(resolvedDocumentNumber);
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        java.util.List<History> history = historyRepository.findByUserDocumentNumberOrderByDateDesc(resolvedDocumentNumber);
        java.util.List<HistoryResponseDto> dtos = history.stream()
                .map(h -> new HistoryResponseDto(h.getId(), h.getLegalProcessId(), h.getActivitySeriesId(), h.getDate(), h.getResult(), h.getCreatedAt(), h.getUserDocumentNumber()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{numeroRadicacion}")
    public ResponseEntity<?> getLegalProcess(
            @PathVariable String numeroRadicacion,
            @RequestParam(name = "SoloActivos", required = false, defaultValue = "false") boolean soloActivos,
            @RequestParam(name = "pagina", required = false, defaultValue = "1") int pagina,
            @RequestHeader(value = "X-Document-Number", required = false) String documentNumberHeader
    ) {
        // Validación de número de radicación (23 dígitos)
        if (!isValidNumeroRadicacion(numeroRadicacion)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "El número de radicación debe tener exactamente 23 dígitos numéricos"));
        }

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("SoloActivos", String.valueOf(soloActivos));
        queryParams.put("pagina", String.valueOf(pagina));

        ResponseEntity<String> response = apiClient.getByNumeroRadicacion(numeroRadicacion, queryParams);

        // Resolver usuario autenticado desde SecurityContext (si el filtro JWT puso Authentication)
        String resolvedDocumentNumber = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            String principal = auth.getName(); // el filtro deja sub (supabase UUID) o email como nombre
            if (principal != null && !principal.isBlank()) {
                // intentar interpretar como UUID (supabase user id)
                try {
                    java.util.UUID supabaseId = java.util.UUID.fromString(principal);
                    Optional<User> u = userRepository.findBySupabaseUserId(supabaseId);
                    if (u.isPresent()) {
                        resolvedDocumentNumber = u.get().getDocumentNumber();
                      }
                } catch (IllegalArgumentException ex) {
                    // no es UUID; intentar por email
                    Optional<User> u2 = userRepository.findByEmail(principal);
                    if (u2.isPresent()) {
                        resolvedDocumentNumber = u2.get().getDocumentNumber();
                      }
                }
            }
        }

        // fallback al header X-Document-Number si no hay usuario autenticado
        if (resolvedDocumentNumber == null && documentNumberHeader != null && !documentNumberHeader.isBlank()) {
            resolvedDocumentNumber = documentNumberHeader;
        }

        // Registrar historial si resolvimos un usuario
        if (resolvedDocumentNumber != null) {
            Optional<User> userOpt = userRepository.findByDocumentNumber(resolvedDocumentNumber);
            if (userOpt.isPresent()) {
                History history = new History();
                history.setUserDocumentNumber(resolvedDocumentNumber);
                // Guardar siempre el idProceso (intentar extraerlo desde la respuesta externa)
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
                    log.warn("Failed to save history for user {} and process {}: {}", resolvedDocumentNumber, numeroRadicacion, e.getMessage());
                }
            }
        }

        // Propagate external response properly
        if (response == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", "No response from external API"));
        }
        if (response.getStatusCode().is2xxSuccessful()) {
            if (response.getBody() != null) {
                return ResponseEntity.ok(response.getBody());
            }
            return ResponseEntity.ok(Map.of("message", "No content from external API"));
        }
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    // Endpoint público que consulta el proceso externo sin persistir historial
    @GetMapping("/public/{numeroRadicacion}")
    public ResponseEntity<?> publicGetLegalProcess(
            @PathVariable String numeroRadicacion,
            @RequestParam(name = "SoloActivos", required = false, defaultValue = "false") boolean soloActivos,
            @RequestParam(name = "pagina", required = false, defaultValue = "1") int pagina
    ) {
        if (!isValidNumeroRadicacion(numeroRadicacion)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "El número de radicación debe tener exactamente 23 dígitos numéricos"));
        }
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("SoloActivos", String.valueOf(soloActivos));
        queryParams.put("pagina", String.valueOf(pagina));
        ResponseEntity<String> response = apiClient.getByNumeroRadicacion(numeroRadicacion, queryParams);
        if (response == null) return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", "No response from external API"));
        if (response.getStatusCode().is2xxSuccessful()) return ResponseEntity.ok(response.getBody() != null ? response.getBody() : Map.of("message","No content from external API"));
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    // Intenta extraer un idProceso corto desde la respuesta externa; si no se encuentra, devuelve numeroRadicacion (y lo registra en logs)
    private String extractIdProcesoFromResponse(String numeroRadicacion, ResponseEntity<String> response) {
        if (response == null || response.getBody() == null) return numeroRadicacion;
        String body = response.getBody();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(body);
            // buscar campos comunes
            List<String> candidates = new ArrayList<>();
            if (root.isObject()) {
                if (root.has("idProceso")) candidates.add(root.get("idProceso").asText());
                if (root.has("id")) candidates.add(root.get("id").asText());
                if (root.has("id_proceso")) candidates.add(root.get("id_proceso").asText());
            }
            // si es array, revisar primer elemento
            if (root.isArray() && !root.isEmpty()) {
                JsonNode first = root.get(0);
                if (first.has("idProceso")) candidates.add(first.get("idProceso").asText());
                if (first.has("id")) candidates.add(first.get("id").asText());
                if (first.has("id_proceso")) candidates.add(first.get("id_proceso").asText());
            }
            // buscar recursivamente claves que parezcan idProceso
            if (candidates.isEmpty()) {
                // recorrer campos de primer nivel buscando un campo corto numérico
                java.util.Iterator<String> it = root.fieldNames();
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
                    // preferir valores significativamente más cortos que numeroRadicacion
                    if (c.length() < numeroRadicacion.length()) return c;
                    // o cualquier no vacío
                    return c;
                }
            }
        } catch (Exception e) {
            log.debug("No se pudo parsear body para extraer idProceso: {}", e.getMessage());
        }
        log.warn("No se pudo extraer idProceso desde la respuesta externa para numeroRadicacion={}, se usará el numero completo en historial", numeroRadicacion);
        return numeroRadicacion;
    }

    @GetMapping("/detail/{idProceso}")
    public ResponseEntity<?> getProcessDetail(
            @PathVariable String idProceso,
            @RequestHeader(value = "X-Document-Number", required = false) String documentNumberHeader
    ) {
        String resolvedId;
        if (isValidNumeroRadicacion(idProceso)) {
            Optional<String> idOpt = apiClient.getProcessIdByNumeroRadicacion(idProceso);
            if (idOpt.isEmpty() || idOpt.get().isBlank()) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("message", "Proceso no encontrado."));
            }
            resolvedId = idOpt.get();
        } else if (isNumeric(idProceso)) {
            resolvedId = idProceso;
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Parámetro inválido: debe ser número de radicación (23 dígitos) o idProceso numérico"));
        }

        ResponseEntity<String> response = apiClient.getProcessDetail(resolvedId);

        String resolvedDocumentNumber = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            String principal = auth.getName();
            if (principal != null && !principal.isBlank()) {
                try {
                    java.util.UUID supabaseId = java.util.UUID.fromString(principal);
                    Optional<User> u = userRepository.findBySupabaseUserId(supabaseId);
                    if (u.isPresent()) {
                        resolvedDocumentNumber = u.get().getDocumentNumber();
                      }
                } catch (IllegalArgumentException ex) {
                    Optional<User> u2 = userRepository.findByEmail(principal);
                    if (u2.isPresent()) {
                        resolvedDocumentNumber = u2.get().getDocumentNumber();
                      }
                }
            }
        }

        if (resolvedDocumentNumber == null && documentNumberHeader != null && !documentNumberHeader.isBlank()) {
            resolvedDocumentNumber = documentNumberHeader;
        }

        if (resolvedDocumentNumber != null) {
            Optional<User> userOpt = userRepository.findByDocumentNumber(resolvedDocumentNumber);
            if (userOpt.isPresent()) {
                History history = new History();
                history.setUserDocumentNumber(resolvedDocumentNumber);
                history.setLegalProcessId(resolvedId);
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
                    log.warn("Failed to save history for user {} and process {}: {}", resolvedDocumentNumber, resolvedId, e.getMessage());
                }
            }
        }

        // Propagate response
        if (response == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", "No response from external API"));
        }
        if (response.getStatusCode().is2xxSuccessful()) {
            if (response.getBody() != null) return ResponseEntity.ok(response.getBody());
            return ResponseEntity.ok(Map.of("message", "No content from external API"));
        }
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    // New endpoint: subjects for a process
    @GetMapping("/{idProceso}/subjects")
    public ResponseEntity<?> getProcessSubjects(
            @PathVariable String idProceso,
            @RequestParam(name = "pagina", required = false, defaultValue = "1") int pagina,
            @RequestHeader(value = "X-Document-Number", required = false) String documentNumberHeader
    ) {
        String resolvedId;
        if (isValidNumeroRadicacion(idProceso)) {
            Optional<String> idOpt = apiClient.getProcessIdByNumeroRadicacion(idProceso);
            if (idOpt.isEmpty() || idOpt.get().isBlank()) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("message", "Proceso no encontrado."));
            }
            resolvedId = idOpt.get();
        } else if (isNumeric(idProceso)) {
            resolvedId = idProceso;
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Parámetro inválido: debe ser número de radicación (23 dígitos) o idProceso numérico"));
        }

        ResponseEntity<String> response = apiClient.getProcessSubjects(resolvedId, pagina);

        // resolve user same as other methods
        String resolvedDocumentNumber = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            String principal = auth.getName();
            if (principal != null && !principal.isBlank()) {
                try {
                    java.util.UUID supabaseId = java.util.UUID.fromString(principal);
                    Optional<User> u = userRepository.findBySupabaseUserId(supabaseId);
                    if (u.isPresent()) resolvedDocumentNumber = u.get().getDocumentNumber();
                } catch (IllegalArgumentException ex) {
                    Optional<User> u2 = userRepository.findByEmail(principal);
                    if (u2.isPresent()) resolvedDocumentNumber = u2.get().getDocumentNumber();
                }
            }
        }
        if (resolvedDocumentNumber == null && documentNumberHeader != null && !documentNumberHeader.isBlank()) {
            resolvedDocumentNumber = documentNumberHeader;
        }

        if (resolvedDocumentNumber != null) {
            Optional<User> userOpt = userRepository.findByDocumentNumber(resolvedDocumentNumber);
            if (userOpt.isPresent()) {
                History history = new History();
                history.setUserDocumentNumber(resolvedDocumentNumber);
                history.setLegalProcessId(resolvedId);
                history.setActivitySeriesId(null);
                history.setDate(OffsetDateTime.now());
                if (response != null && response.getBody() != null && !response.getBody().isBlank()) {
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
                    log.warn("Failed to save history for user {} and process {}: {}", resolvedDocumentNumber, resolvedId, e.getMessage());
                }
            }
        }

        // Propagate external response
        if (response == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", "No response from external API"));
        }
        if (response.getStatusCode().is2xxSuccessful()) {
            if (response.getBody() != null && !response.getBody().isBlank()) return ResponseEntity.ok(response.getBody());
            return ResponseEntity.ok(Map.of("message", "No subjects associated with this process"));
        }
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    // New endpoint: documents for a process
    @GetMapping("/{idProceso}/documents")
    public ResponseEntity<?> getProcessDocuments(
            @PathVariable String idProceso,
            @RequestHeader(value = "X-Document-Number", required = false) String documentNumberHeader
    ) {
        String resolvedId;
        if (isValidNumeroRadicacion(idProceso)) {
            Optional<String> idOpt = apiClient.getProcessIdByNumeroRadicacion(idProceso);
            if (idOpt.isEmpty() || idOpt.get().isBlank()) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("message", "Proceso no encontrado."));
            }
            resolvedId = idOpt.get();
        } else if (isNumeric(idProceso)) {
            resolvedId = idProceso;
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Parámetro inválido: debe ser número de radicación (23 dígitos) o idProceso numérico"));
        }

        ResponseEntity<String> response = apiClient.getProcessDocuments(resolvedId);

        // resolve user
        String resolvedDocumentNumber = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            String principal = auth.getName();
            if (principal != null && !principal.isBlank()) {
                try {
                    java.util.UUID supabaseId = java.util.UUID.fromString(principal);
                    Optional<User> u = userRepository.findBySupabaseUserId(supabaseId);
                    if (u.isPresent()) resolvedDocumentNumber = u.get().getDocumentNumber();
                } catch (IllegalArgumentException ex) {
                    Optional<User> u2 = userRepository.findByEmail(principal);
                    if (u2.isPresent()) resolvedDocumentNumber = u2.get().getDocumentNumber();
                }
            }
        }
        if (resolvedDocumentNumber == null && documentNumberHeader != null && !documentNumberHeader.isBlank()) {
            resolvedDocumentNumber = documentNumberHeader;
        }

        if (resolvedDocumentNumber != null) {
            Optional<User> userOpt = userRepository.findByDocumentNumber(resolvedDocumentNumber);
            if (userOpt.isPresent()) {
                History history = new History();
                history.setUserDocumentNumber(resolvedDocumentNumber);
                history.setLegalProcessId(resolvedId);
                history.setActivitySeriesId(null);
                history.setDate(OffsetDateTime.now());
                if (response != null && response.getBody() != null && !response.getBody().isBlank()) {
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
                    log.warn("Failed to save history for user {} and process {}: {}", resolvedDocumentNumber, idProceso, e.getMessage());
                }
            }
        }

        // Propagate external response
        if (response == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", "No response from external API"));
        }
        if (response.getStatusCode().is2xxSuccessful()) {
            if (response.getBody() != null && !response.getBody().isBlank()) return ResponseEntity.ok(response.getBody());
            return ResponseEntity.ok(Map.of("message", "El proceso no tiene documentos asociados"));
        }
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @GetMapping("/{idProceso}/actuaciones")
    public ResponseEntity<?> getProcessActuaciones(
            @PathVariable String idProceso,
            @RequestParam(name = "pagina", required = false, defaultValue = "1") int pagina,
            @RequestHeader(value = "X-Document-Number", required = false) String documentNumberHeader
    ) {
        String resolvedId;
        if (isValidNumeroRadicacion(idProceso)) {
            Optional<String> idOpt = apiClient.getProcessIdByNumeroRadicacion(idProceso);
            if (idOpt.isEmpty() || idOpt.get().isBlank()) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("message", "Proceso no encontrado."));
            }
            resolvedId = idOpt.get();
        } else if (isNumeric(idProceso)) {
            resolvedId = idProceso;
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Parámetro inválido."));
        }
        //llamada a la API -> apiclient
        ResponseEntity<String> response = apiClient.getProcessActuaciones(resolvedId, pagina);
        // podemos registrar el hisotrial que el user vio actucin logica de getprocessdetaiñ
        if (response == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", "Sin respuesta de la Rama Judicial"));
        }
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }
}

