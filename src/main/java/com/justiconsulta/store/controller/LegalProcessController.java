package com.justiconsulta.store.controller;

import com.justiconsulta.store.model.LegalProcess;
import com.justiconsulta.store.model.User;
import com.justiconsulta.store.repository.LegalProcessRepository;
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

    public LegalProcessController(LegalProcessRepository legalProcessRepository, ApiClient apiClient, UserRepository userRepository) {
        this.legalProcessRepository = legalProcessRepository;
        this.apiClient = apiClient;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<LegalProcess> getAllLegalProcesses() {
        return legalProcessRepository.findAll();
    }


    @GetMapping("/{numeroRadicacion}")
    public ResponseEntity<?> getLegalProcess(
            @PathVariable String numeroRadicacion,
            @RequestParam Map<String, String> queryParams) {

        ResponseEntity<String> response = apiClient.getByNumeroRadicacion(numeroRadicacion, queryParams);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return ResponseEntity.ok(response.getBody());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{numeroRadicacion}")
    public ResponseEntity<?> associateProcessToUser(
            @PathVariable String numeroRadicacion,
            @RequestBody LegalProcess payload
    ) {

        String document_number = payload.getUser() != null ? payload.getUser().getDocumentNumber() : null;
        if (document_number == null) {
            return ResponseEntity.badRequest().body("El usuario (document_number) es obligatorio.");
        }
        Optional<User> user = userRepository.findByDocumentNumber(document_number);
        if (user.isEmpty()) {
            return ResponseEntity.unprocessableEntity().body("Usuario no encontrado.");
        }
        payload.setUser(user.get());
        if (payload.getCreatedAt() == null) {
            payload.setCreatedAt(OffsetDateTime.now());
        }

        payload.setId(UUID.fromString(numeroRadicacion));
        return createAndPersistLegalProcess(payload);
    }

    private ResponseEntity<?> createAndPersistLegalProcess(LegalProcess legalProcess) {
        if (legalProcess.getId() == null) {
            return ResponseEntity.badRequest().body("El id es obligatorio.");
        }
        if (legalProcessRepository.existsById(legalProcess.getId())) {
            return ResponseEntity.unprocessableEntity().body("El id ya existe.");
        }
        boolean valido = apiClient.validateId(legalProcess.getId().toString());
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
