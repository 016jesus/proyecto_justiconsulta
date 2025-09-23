package com.justiconsulta.store.controller;

import com.justiconsulta.store.model.User;
import com.justiconsulta.store.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User payload) {
        var result = authService.register(payload);
        return ResponseEntity.ok(result); // result puede ser el usuario creado o un DTO con token
    }
}
