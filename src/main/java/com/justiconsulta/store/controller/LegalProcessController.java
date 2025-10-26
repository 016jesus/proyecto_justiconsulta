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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/legal-processes")
public class LegalProcessController {
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

        // Registrar historial si se proporcionó un usuario
        if (documentNumberHeader != null && !documentNumberHeader.isBlank()) {
            // sólo registrar si el usuario existe
            Optional<User> userOpt = userRepository.findByDocumentNumber(documentNumberHeader);
            if (userOpt.isPresent()) {
                History history = new History();
                history.setUserDocumentNumber(documentNumberHeader);
                history.setLegalProcessId(numeroRadicacion);
                history.setActivitySeriesId(null);
                history.setDate(OffsetDateTime.now());
                if (response != null && response.getBody() != null) {
                    // acotar el tamaño del resultado para evitar textos demasiado grandes
                    String body = response.getBody();
                    history.setResult(body.length() > 2000 ? body.substring(0, 2000) : body);
                } else if (response != null) {
                    history.setResult("HTTP " + response.getStatusCodeValue());
                } else {
                    history.setResult("No response from external API");
                }
                history.setCreatedAt(OffsetDateTime.now());
                historyRepository.save(history);
            }
        }

        if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return ResponseEntity.ok(response.getBody());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // DTO para asociar proceso a usuario
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
}
