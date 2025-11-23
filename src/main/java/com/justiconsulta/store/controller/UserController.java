package com.justiconsulta.store.controller;

import com.justiconsulta.store.dto.request.DocumentNumberRequest;
import com.justiconsulta.store.dto.request.EmailRequest;
import com.justiconsulta.store.model.User;
import com.justiconsulta.store.service.contract.IUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {
    private final IUserService userService;

    public UserController(IUserService userService) {
        this.userService = userService;
    }

    @PostMapping("/by-document")
    public ResponseEntity<User> getUserByDocument(@Valid @RequestBody DocumentNumberRequest request) {
        return userService.getUserByDocument(request.getDocumentNumber());
    }

    @PostMapping("/by-email")
    public ResponseEntity<User> getUserByEmail(@Valid @RequestBody EmailRequest request) {
        return userService.getUserByEmail(request.getEmail());
    }

    @GetMapping("/legal-process-ids")
    public ResponseEntity<List<String>> getLegalProcessIdsByUser(@Valid @RequestBody DocumentNumberRequest request) {
        return userService.getLegalProcessIdsByUser(request.getDocumentNumber());
    }
}
