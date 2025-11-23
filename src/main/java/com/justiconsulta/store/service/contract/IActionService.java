package com.justiconsulta.store.service.contract;

import com.justiconsulta.store.model.Action;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public interface IActionService {
    List<Action> getAllActions();
    Action createActionAndNotify(UUID legalProcessId, String description);
    ResponseEntity<?> getActuaciones(String idProceso, int pagina);
}