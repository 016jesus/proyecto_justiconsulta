package com.justiconsulta.store.controller;

import com.justiconsulta.store.dto.response.HistoryResponseDto;
import com.justiconsulta.store.dto.response.LegalProcessResponseDto;
import com.justiconsulta.store.service.contract.ILegalProcessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/legal-processes")
public class LegalProcessController {
    private final ILegalProcessService legalProcessService;

    public LegalProcessController(ILegalProcessService legalProcessService) {
        this.legalProcessService = legalProcessService;
    }

    @DeleteMapping("/{numeroRadicacion}")
    public ResponseEntity<?> removeAssociation(@PathVariable String numeroRadicacion) {
        return legalProcessService.removeAssociation(numeroRadicacion);
    }

    @GetMapping
    public ResponseEntity<List<LegalProcessResponseDto>> getAllLegalProcesses() {
        return legalProcessService.getAllLegalProcesses();
    }

    @PostMapping("/{numeroRadicacion}")
    public ResponseEntity<?> associateProcessToUser(@PathVariable String numeroRadicacion) {
        return legalProcessService.associateProcessToUser(numeroRadicacion);
    }

    @GetMapping("/history")
    public ResponseEntity<List<HistoryResponseDto>> getUserHistory() {
        return legalProcessService.getUserHistory();
    }

    @GetMapping("/{numeroRadicacion}")
    public ResponseEntity<?> getLegalProcess(
            @PathVariable String numeroRadicacion,
            @RequestParam(name = "SoloActivos", required = false, defaultValue = "false") boolean soloActivos,
            @RequestParam(name = "pagina", required = false, defaultValue = "1") int pagina,
            @RequestHeader(value = "X-Document-Number", required = false) String documentNumberHeader
    ) {
        return legalProcessService.getLegalProcess(numeroRadicacion, soloActivos, pagina, documentNumberHeader);
    }

    @GetMapping("/public/{numeroRadicacion}")
    public ResponseEntity<?> publicGetLegalProcess(
            @PathVariable String numeroRadicacion,
            @RequestParam(name = "SoloActivos", required = false, defaultValue = "false") boolean soloActivos,
            @RequestParam(name = "pagina", required = false, defaultValue = "1") int pagina
    ) {
        return legalProcessService.publicGetLegalProcess(numeroRadicacion, soloActivos, pagina);
    }

    @GetMapping("/{idProceso}/detail")
    public ResponseEntity<?> getProcessDetail(@PathVariable String idProceso) {
        return legalProcessService.getProcessDetail(idProceso);
    }

    @GetMapping("/{idProceso}/subjects")
    public ResponseEntity<?> getProcessSubjects(
            @PathVariable String idProceso,
            @RequestParam(name = "pagina", required = false, defaultValue = "1") int pagina
    ) {
        return legalProcessService.getProcessSubjects(idProceso, pagina);
    }

    @GetMapping("/{idProceso}/documents")
    public ResponseEntity<?> getProcessDocuments(@PathVariable String idProceso) {
        return legalProcessService.getProcessDocuments(idProceso);
    }

    @GetMapping("/{idProceso}/actuaciones")
    public ResponseEntity<?> getProcessActuaciones(
            @PathVariable String idProceso,
            @RequestParam(name = "pagina", required = false, defaultValue = "1") int pagina
    ) {
        return legalProcessService.getProcessActuaciones(idProceso, pagina);
    }
}
