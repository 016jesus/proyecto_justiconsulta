package com.justiconsulta.store.service;

import com.justiconsulta.store.dto.request.RegisterRequestDTO;
import com.justiconsulta.store.exception.ResourceNotFoundException;
import com.justiconsulta.store.model.User;
import com.justiconsulta.store.repository.UserRepository;
import com.justiconsulta.store.security.TokenValidationResult;
import com.justiconsulta.store.security.TokenValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final SupabaseClient supabaseClient;
    private final TokenValidator tokenValidator;

    public String login(String email, String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

        if (user.getEncryptedPassword() == null || !BCrypt.checkpw(rawPassword, user.getEncryptedPassword())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }

        return jwtTokenService.generate(user.getDocumentNumber(), user.getEmail());
    }


    public Object register(RegisterRequestDTO payload) {

        if (userRepository.findByDocumentNumber(payload.documentNumber()).isPresent()) {
            throw new IllegalArgumentException("El número de documento ya está registrado.");
        }
        if (userRepository.findByEmail(payload.email()).isPresent()) {
            throw new IllegalArgumentException("El correo electrónico ya está registrado.");
        }

        // Prepare metadata to send to Supabase
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("document_number", payload.documentNumber());
        metadata.put("document_type", payload.documentType());
        metadata.put("first_name", payload.firstName());
        metadata.put("middle_name", payload.middleName());
        metadata.put("first_last_name", payload.firstLastName());
        metadata.put("second_last_name", payload.secondLastName());

        // Create user in Supabase (admin API). This will return the supabase user id as UUID
        UUID supabaseUserId = supabaseClient.createUser(payload.email(), payload.password(), metadata);

        // Hash password locally for backward compatibility (optional). If you want to fully delegate auth to Supabase,
        // consider setting encryptedPassword to null and update login flow accordingly.
        String rawPassword = payload.password();
        String hashed = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));

        User newUser = new User();

        newUser.setDocumentType(payload.documentType());
        if (payload.birthDate() != null && !payload.birthDate().isEmpty()) {
            newUser.setBirthDate(java.sql.Date.valueOf(payload.birthDate())); // Convertimos String a java.sql.Date
        }

        newUser.setDocumentNumber(payload.documentNumber());
        newUser.setFirstName(payload.firstName());
        newUser.setMiddleName(payload.middleName());
        newUser.setFirstLastName(payload.firstLastName());
        newUser.setSecondLastName(payload.secondLastName());
        newUser.setEmail(payload.email());
        newUser.setEncryptedPassword(hashed);
        newUser.setSupabaseUserId(supabaseUserId);

        return userRepository.save(newUser);
    }

    // New: trigger password recovery flow via Supabase for existing users
    public void sendPasswordRecovery(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        // verify user exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with provided email not found"));

        // Delegate to Supabase client
        supabaseClient.sendPasswordRecovery(email);
    }

    // New: update password using Supabase access token; also sync local encrypted password
    public void updatePasswordFromAccessToken(String accessToken, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("La contraseña es requerida");
        }
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        }
        // Validate token and get principal (email or supabase user id)
        TokenValidationResult result = tokenValidator.validate(accessToken);
        if (result == null || !result.isValid() || result.getPrincipal() == null || result.getPrincipal().isBlank()) {
            throw new IllegalArgumentException("Token inválido o expirado");
        }
        String principal = result.getPrincipal();

        // Update password in Supabase using the access token
        supabaseClient.updatePasswordWithAccessToken(accessToken, newPassword);

        // Update password locally (if user exists)
        Optional<User> userOpt;
        try {
            UUID supabaseId = UUID.fromString(principal);
            userOpt = userRepository.findBySupabaseUserId(supabaseId);
        } catch (IllegalArgumentException ex) {
            userOpt = userRepository.findByEmail(principal);
        }

        if (userOpt.isPresent()) {
            User u = userOpt.get();
            String hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
            u.setEncryptedPassword(hashed);
            userRepository.save(u);
        }
        // If not present locally, we silently ignore to avoid leaking info. Supabase update already succeeded.
    }
}
