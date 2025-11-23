package com.justiconsulta.store.service.contract;

import com.justiconsulta.store.dto.response.HistoryResponseDto;
import com.justiconsulta.store.dto.response.LegalProcessResponseDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ILegalProcessService {
    ResponseEntity<?> removeAssociation(String numeroRadicacion);
    ResponseEntity<List<LegalProcessResponseDto>> getAllLegalProcesses();
    ResponseEntity<?> associateProcessToUser(String numeroRadicacion);
    ResponseEntity<List<HistoryResponseDto>> getUserHistory();
    ResponseEntity<?> getLegalProcess(String numeroRadicacion, boolean soloActivos, int pagina, String documentNumberHeader);
    ResponseEntity<?> publicGetLegalProcess(String numeroRadicacion, boolean soloActivos, int pagina);
    ResponseEntity<?> getProcessDetail(String idProceso);
    ResponseEntity<?> getProcessSubjects(String idProceso, int pagina);
    ResponseEntity<?> getProcessDocuments(String idProceso);
    ResponseEntity<?> getProcessActuaciones(String idProceso, int pagina);
}
