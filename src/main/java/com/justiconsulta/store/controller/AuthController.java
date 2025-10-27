package com.justiconsulta.store.controller;


import com.justiconsulta.store.dto.request.RegisterRequestDTO;
import com.justiconsulta.store.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.getEmail().trim(), request.getPassword());
        TokenResponse resp = new TokenResponse();
        resp.setToken(token);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO registerRequest) {

        Object savedUser = authService.register(registerRequest);

        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<EmailResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String email = request.getEmail().trim();
        authService.sendPasswordRecovery(email);
        EmailResponse resp = new EmailResponse();
        resp.setEmail(email);
        return ResponseEntity.accepted().body(resp);
    }

    @Data
    public static class LoginRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
    }

    @Data
    public static class TokenResponse {
        private String token;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank @Email
        private String email;
    }

    @Data
    public static class EmailResponse {
        private String email;
    }
}