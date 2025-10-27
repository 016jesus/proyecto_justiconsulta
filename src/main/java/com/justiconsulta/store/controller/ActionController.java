package com.justiconsulta.store.controller;

import com.justiconsulta.store.model.Action;
import com.justiconsulta.store.model.History;
import com.justiconsulta.store.model.User;
import com.justiconsulta.store.repository.ActionRepository;
import com.justiconsulta.store.repository.HistoryRepository;
import com.justiconsulta.store.repository.UserRepository;
import com.justiconsulta.store.service.ActuationService;
import com.justiconsulta.store.service.ApiClient;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/actions")
public class ActionController {
    private final ActionRepository actionRepository;
    private final ActuationService actuationService;
    private final ApiClient apiClient;
    private final UserRepository userRepository;
    private final HistoryRepository historyRepository;

    public ActionController(ActionRepository actionRepository, ActuationService actuationService, ApiClient apiClient, UserRepository userRepository, HistoryRepository historyRepository) {
        this.actionRepository = actionRepository;
        this.actuationService = actuationService;
        this.apiClient = apiClient;
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
    }

    @GetMapping
    public List<Action> getAllActions() {
        return actionRepository.findAll();
    }

    // Endpoint para crear una acción y notificar
    @PostMapping("/legal-process/{legalProcessId}")
    public Action createActionAndNotify(@PathVariable UUID legalProcessId, @RequestBody String description) {
        return actuationService.createActionAndNotify(legalProcessId, description);
    }

    @GetMapping("/{idProceso}")
    public ResponseEntity<?> getActuaciones(
            @PathVariable String idProceso,
            @RequestParam(name = "pagina", required = false, defaultValue = "1") int pagina,
            @RequestHeader(value = "X-Document-Number", required = false) String documentNumberHeader
    ) {
        ResponseEntity<String> response = apiClient.getProcessActuaciones(idProceso, pagina);

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
                historyRepository.save(history);
            }
        }

        if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isBlank()) {
            return ResponseEntity.ok(response.getBody());
        } else if (response != null && response.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.ok(java.util.Map.of("message", "No actuaciones asociadas a este proceso"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
