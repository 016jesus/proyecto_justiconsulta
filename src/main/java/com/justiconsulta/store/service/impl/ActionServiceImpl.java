package com.justiconsulta.store.service.impl;

import com.justiconsulta.store.model.Action;
import com.justiconsulta.store.repository.ActionRepository;
import com.justiconsulta.store.service.ActuationService;
import com.justiconsulta.store.service.ApiClient;
import com.justiconsulta.store.service.contract.IActionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ActionServiceImpl implements IActionService {
    private final ActionRepository actionRepository;
    private final ActuationService actuationService;
    private final ApiClient apiClient;

    public ActionServiceImpl(ActionRepository actionRepository, ActuationService actuationService, ApiClient apiClient) {
        this.actionRepository = actionRepository;
        this.actuationService = actuationService;
        this.apiClient = apiClient;
    }

    @Override
    public List<Action> getAllActions() {
        return actionRepository.findAll();
    }

    @Override
    public Action createActionAndNotify(UUID legalProcessId, String description) {
        return actuationService.createActionAndNotify(legalProcessId, description);
    }

    @Override
    public ResponseEntity<?> getActuaciones(String idProceso, int pagina) {
        String resolvedId;
        if (isValidNumeroRadicacion(idProceso)) {
            Optional<String> idOpt = apiClient.getProcessIdByNumeroRadicacion(idProceso);
            if (idOpt.isEmpty() || idOpt.get().isBlank()) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(java.util.Map.of("message", "Proceso no encontrado."));
            }
            resolvedId = idOpt.get();
        } else if (isNumeric(idProceso)) {
            resolvedId = idProceso;
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("message", "Parámetro inválido: debe ser número de radicación (23 dígitos) o idProceso numérico"));
        }

        ResponseEntity<String> response = apiClient.getProcessActuaciones(resolvedId, pagina);

        if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isBlank()) {
            return ResponseEntity.ok(response.getBody());
        } else if (response != null && response.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.ok(java.util.Map.of("message", "No actuaciones asociadas a este proceso"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private boolean isValidNumeroRadicacion(String value) {
        return value != null && value.matches("\\d{23}");
    }

    private boolean isNumeric(String value) {
        return value != null && value.matches("\\d+");
    }
}

