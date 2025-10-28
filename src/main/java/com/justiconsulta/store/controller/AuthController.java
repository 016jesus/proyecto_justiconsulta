package com.justiconsulta.store.controller;


import com.justiconsulta.store.dto.request.RegisterRequestDTO;
import com.justiconsulta.store.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    // Nuevo endpoint: actualiza la contraseña usando access_token (Bearer) de Supabase
    @PostMapping(value = "/update-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updatePassword(@Valid @RequestBody UpdatePasswordRequest request,
                                            @RequestHeader(value = "Authorization", required = true) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Falta token Bearer en Authorization");
        }
        String accessToken = authorization.substring(7);
        authService.updatePasswordFromAccessToken(accessToken, request.getPassword());
        return ResponseEntity.noContent().build();
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

    @Data
    public static class UpdatePasswordRequest {
        @NotBlank
        private String password;
    }
}