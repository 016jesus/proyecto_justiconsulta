package com.justiconsulta.store.controller;

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
                history.setLegalProcessId(numeroRadicacion);
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

    @GetMapping("/detail/{idProceso}")
    public ResponseEntity<?> getProcessDetail(
            @PathVariable String idProceso,
            @RequestHeader(value = "X-Document-Number", required = false) String documentNumberHeader
    ) {
        ResponseEntity<String> response = apiClient.getProcessDetail(idProceso);


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
                history.setLegalProcessId(idProceso);
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
                    log.warn("Failed to save history for user {} and process {}: {}", resolvedDocumentNumber, idProceso, e.getMessage());
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

    // New endpoint: subjects for a process
    @GetMapping("/{idProceso}/subjects")
    public ResponseEntity<?> getProcessSubjects(
            @PathVariable String idProceso,
            @RequestParam(name = "pagina", required = false, defaultValue = "1") int pagina,
            @RequestHeader(value = "X-Document-Number", required = false) String documentNumberHeader
    ) {
        ResponseEntity<String> response = apiClient.getProcessSubjects(idProceso, pagina);

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
                history.setLegalProcessId(idProceso);
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
        ResponseEntity<String> response = apiClient.getProcessDocuments(idProceso);

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
                history.setLegalProcessId(idProceso);
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
}
