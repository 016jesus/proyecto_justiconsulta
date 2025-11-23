package com.justiconsulta.store.controller;

import com.justiconsulta.store.model.Action;
import com.justiconsulta.store.service.contract.IActionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/actions")
public class ActionController {
    private final IActionService actionService;

    public ActionController(IActionService actionService) {
        this.actionService = actionService;
    }

    @GetMapping
    public List<Action> getAllActions() {
        return actionService.getAllActions();
    }

    // Endpoint para crear una acción y notificar
    @PostMapping("/legal-process/{legalProcessId}")
    public Action createActionAndNotify(@PathVariable UUID legalProcessId, @RequestBody String description) {
        return actionService.createActionAndNotify(legalProcessId, description);
    }

    @GetMapping("/{idProceso}")
    public ResponseEntity<?> getActuaciones(
            @PathVariable String idProceso,
            @RequestParam(name = "pagina", required = false, defaultValue = "1") int pagina
    ) {
        return actionService.getActuaciones(idProceso, pagina);
    }
}
