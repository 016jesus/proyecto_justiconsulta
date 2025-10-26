package com.justiconsulta.store.controller;

import com.justiconsulta.store.model.User;
import com.justiconsulta.store.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.justiconsulta.store.repository.UserLegalProcessRepository;
import com.justiconsulta.store.dto.request.DocumentNumberRequest;
import com.justiconsulta.store.dto.request.EmailRequest;


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

    @PostMapping("/by-document")
    public ResponseEntity<User> getUserByDocument(@Valid @RequestBody DocumentNumberRequest request) {
        return userRepository.findById(request.getDocumentNumber())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/by-email")
    public ResponseEntity<User> getUserByEmail(@Valid @RequestBody EmailRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/legal-process-ids")
    public ResponseEntity<List<String>> getLegalProcessIdsByUser(@Valid @RequestBody DocumentNumberRequest request) {
        List<String> processIds = userLegalProcessRepository.findProcessIdsByUserDocumentNumber(request.getDocumentNumber());
        return ResponseEntity.ok(processIds);
    }

}
