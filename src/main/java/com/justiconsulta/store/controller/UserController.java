package com.justiconsulta.store.controller;

import com.justiconsulta.store.model.User;
import com.justiconsulta.store.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

import com.justiconsulta.store.repository.UserLegalProcessRepository;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {
    private final UserRepository userRepository;
    private final UserLegalProcessRepository userLegalProcessRepository;

    public UserController(UserRepository userRepository, UserLegalProcessRepository userLegalProcessRepository) {
        this.userRepository = userRepository;
        this.userLegalProcessRepository = userLegalProcessRepository;
    }

    // Nuevo endpoint: buscar usuario por documentNumber en el body
    @PostMapping("/by-document")
    public ResponseEntity<User> getUserByDocument(@Valid @RequestBody DocumentNumberRequest request) {
        if (request.getDocumentNumber() == null || request.getDocumentNumber().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return userRepository.findById(request.getDocumentNumber())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DTO para recibir el documentNumber en el body
    public static class DocumentNumberRequest {
        private String documentNumber;
        public String getDocumentNumber() { return documentNumber; }
        public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    }

    @GetMapping("/legal-process-ids")
    public ResponseEntity<List<String>> getLegalProcessIdsByUser(@Valid @RequestBody DocumentNumberRequest request) {
        if (request.getDocumentNumber() == null || request.getDocumentNumber().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        List<String> processIds = userLegalProcessRepository.findProcessIdsByUserDocumentNumber(request.getDocumentNumber());
        return ResponseEntity.ok(processIds);
    }

}
