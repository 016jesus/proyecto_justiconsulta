package com.justiconsulta.store.controller;

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
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;

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

    @GetMapping
    public List<LegalProcess> getAllLegalProcesses() {
        return legalProcessRepository.findAll();
    }

    @GetMapping("/{numeroRadicacion}")
    public ResponseEntity<?> getLegalProcess(
            @PathVariable String numeroRadicacion,
            @RequestParam(name = "SoloActivos", required = false, defaultValue = "false") boolean soloActivos,
            @RequestParam(name = "pagina", required = false, defaultValue = "1") int pagina,
            @RequestHeader(value = "X-Document-Number", required = false) String documentNumberHeader
    ) {

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
                    // acotar el tamaño del resultado para evitar textos demasiado grandes
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


    @Data
    public static class AssociateProcessRequest {
        private String documentNumber;
    }

    @PostMapping("/{numeroRadicacion}")
    public ResponseEntity<?> associateProcessToUser(
            @PathVariable String numeroRadicacion,
            @RequestBody AssociateProcessRequest request
    ) {
        String documentNumber = request.getDocumentNumber();
        if (documentNumber == null || documentNumber.isBlank()) {
            return ResponseEntity.badRequest().body("El usuario (documentNumber) es obligatorio.");
        }
        Optional<User> user = userRepository.findByDocumentNumber(documentNumber);
        if (user.isEmpty()) {
            return ResponseEntity.unprocessableEntity().body("Usuario no encontrado.");
        }
        Optional<LegalProcess> process = legalProcessRepository.findById(new LegalProcess.LegalProcessId(numeroRadicacion, documentNumber));
        if (process.isEmpty()) {
            return ResponseEntity.unprocessableEntity().body("Proceso no encontrado.");
        }
        UserLegalProcessId id = new UserLegalProcessId(documentNumber, numeroRadicacion);
        if (userLegalProcessRepository.existsById(id)) {
            return ResponseEntity.unprocessableEntity().body("La asociación ya existe.");
        }
        UserLegalProcess association = new UserLegalProcess(id, null);
        userLegalProcessRepository.save(association);
        return ResponseEntity.ok("Asociación creada correctamente.");
    }

    @GetMapping("/history")
    public ResponseEntity<?> getUserHistory(@RequestHeader(value = "X-Document-Number") String documentNumber) {
        if (documentNumber == null || documentNumber.isBlank()) {
            return ResponseEntity.badRequest().body("X-Document-Number header is required.");
        }
        Optional<User> user = userRepository.findByDocumentNumber(documentNumber);
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<History> history = historyRepository.findByUserDocumentNumberOrderByDateDesc(documentNumber);
        return ResponseEntity.ok(history);
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
