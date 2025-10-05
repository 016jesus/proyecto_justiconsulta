package com.justiconsulta.store.controller;

import com.justiconsulta.store.model.LegalProcess;
import com.justiconsulta.store.model.User;
import com.justiconsulta.store.model.UserLegalProcess;
import com.justiconsulta.store.model.UserLegalProcess.UserLegalProcessId;
import com.justiconsulta.store.repository.LegalProcessRepository;
import com.justiconsulta.store.repository.UserLegalProcessRepository;
import com.justiconsulta.store.repository.UserRepository;
import com.justiconsulta.store.service.ApiClient;
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

    public LegalProcessController(LegalProcessRepository legalProcessRepository, ApiClient apiClient, UserRepository userRepository, UserLegalProcessRepository userLegalProcessRepository) {
        this.legalProcessRepository = legalProcessRepository;
        this.apiClient = apiClient;
        this.userRepository = userRepository;
        this.userLegalProcessRepository = userLegalProcessRepository;
    }

    @GetMapping
    public List<LegalProcess> getAllLegalProcesses() {
        return legalProcessRepository.findAll();
    }



    @GetMapping("/{numeroRadicacion}")
    public ResponseEntity<?> getLegalProcess(
            @PathVariable String numeroRadicacion,
            @RequestParam(name = "SoloActivos", required = true, defaultValue = "false") boolean soloActivos,
            @RequestParam(name = "pagina", required = false, defaultValue = "1") int pagina) {

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("SoloActivos", String.valueOf(soloActivos));
        queryParams.put("pagina", String.valueOf(pagina));

        ResponseEntity<String> response = apiClient.getByNumeroRadicacion(numeroRadicacion, queryParams);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return ResponseEntity.ok(response.getBody());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // DTO para asociar proceso a usuario
    public static class AssociateProcessRequest {
        private String documentNumber;
        public String getDocumentNumber() { return documentNumber; }
        public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
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

    private ResponseEntity<?> createAndPersistLegalProcess(LegalProcess legalProcess) {
        if (legalProcess.getId() == null || legalProcess.getId().getId() == null || legalProcess.getId().getUserDocumentNumber() == null) {
            return ResponseEntity.badRequest().body("El id compuesto (id y user_document_number) es obligatorio.");
        }
        // Verificar existencia por PK compuesta
        if (legalProcessRepository.existsById(legalProcess.getId())) {
            return ResponseEntity.unprocessableEntity().body("El id ya existe.");
        }
        boolean valido = apiClient.validateId(legalProcess.getId().getId());
        if (!valido) {
            return ResponseEntity.unprocessableEntity().body("El id no es válido según ApiClient.");
        }
        if (legalProcess.getCreatedAt() == null) {
            legalProcess.setCreatedAt(OffsetDateTime.now());
        }
        LegalProcess saved = legalProcessRepository.save(legalProcess);
        return ResponseEntity.ok(saved);
    }
}
