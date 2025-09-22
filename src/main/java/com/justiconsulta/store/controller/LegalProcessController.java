package com.justiconsulta.store.controller;

import com.justiconsulta.store.model.LegalProcess;
import com.justiconsulta.store.repository.LegalProcessRepository;
import com.justiconsulta.store.service.ApiClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/legal-processes")
public class LegalProcessController {
    private final LegalProcessRepository legalProcessRepository;
    private final ApiClient apiClient;

    public LegalProcessController(LegalProcessRepository legalProcessRepository, ApiClient apiClient) {
        this.legalProcessRepository = legalProcessRepository;
        this.apiClient = apiClient;
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
}
