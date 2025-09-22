package com.justiconsulta.store.controller;

import com.justiconsulta.store.model.Action;
import com.justiconsulta.store.service.ActuationService;
import com.justiconsulta.store.repository.ActionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/actions")
public class ActionController {
    private final ActionRepository actionRepository;
    private final ActuationService actuationService;

    public ActionController(ActionRepository actionRepository, ActuationService actuationService) {
        this.actionRepository = actionRepository;
        this.actuationService = actuationService;
    }

    @GetMapping
    public List<Action> getAllActions() {
        return actionRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Action> getAction(@PathVariable String id) {
        return actionRepository.findById(UUID.fromString(id))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Endpoint para crear una acción y notificar
    @PostMapping("/legal-process/{legalProcessId}")
    public Action createActionAndNotify(@PathVariable UUID legalProcessId, @RequestBody String description) {
        return actuationService.createActionAndNotify(legalProcessId, description);
    }
}

