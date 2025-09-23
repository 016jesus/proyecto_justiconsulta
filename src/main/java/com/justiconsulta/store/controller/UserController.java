package com.justiconsulta.store.controller;

import com.justiconsulta.store.model.User;
import com.justiconsulta.store.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.justiconsulta.store.service.EmailService;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {
    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;

    }

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/{documentNumber}")
    public ResponseEntity<User> getUser(@PathVariable String documentNumber) {
        return userRepository.findById(documentNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        // Validación: no crear si ya existe
        if (userRepository.existsById(user.getDocumentNumber())) {
            return ResponseEntity.status(409).build(); // Conflict
        }
        User savedUser = userRepository.save(user);
        return ResponseEntity.status(201).body(savedUser);
    }
}
