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

    @PostMapping
    public LegalProcess createLegalProcess(@RequestBody LegalProcess legalProcess) {
        return legalProcessRepository.save(legalProcess);
    }

    @PostMapping("/users/{document_number}")
    public ResponseEntity<LegalProcess> associateProcessToUser(
            @PathVariable String document_number,
            @RequestBody LegalProcess payload
    ) {
        // 1) Cargar o referenciar el usuario (asumiendo que solo se necesita su id)
        Optional<User> user = userRepository.findByDocumentNumber(document_number);
        // 2) Construir la entidad LegalProcess
        LegalProcess entity = new LegalProcess();
        entity.setUser(user.orElseThrow());
        entity.setLastActionDate(payload.getLastActionDate());
        entity.setCreatedAt(OffsetDateTime.now());

        // 3) Guardar
        LegalProcess saved = legalProcessRepository.save(entity);

        // 4) Notificar (ej. usando un servicio de notificaciones)
        // notificationService.createForLegalProcess(saved);

        return ResponseEntity.ok(saved);
    }
}
