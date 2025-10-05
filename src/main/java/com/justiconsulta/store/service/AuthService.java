package com.justiconsulta.store.service;

import com.justiconsulta.store.model.User;
import com.justiconsulta.store.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;

    public String login(String email, String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

        if (user.getEncryptedPassword() == null || !BCrypt.checkpw(rawPassword, user.getEncryptedPassword())) {
            throw new IllegalArgumentException("Credenciales invalidad");
        }

        return jwtTokenService.generate(user.getDocumentNumber(), user.getEmail());
    }


    public Object register(User payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload requerido.");
        }
        if (payload.getDocumentNumber() == null || payload.getDocumentNumber().isBlank()) {
            throw new IllegalArgumentException("documentNumber es obligatorio.");
        }

        Optional<User> existing = userRepository.findByDocumentNumber(payload.getDocumentNumber());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("El usuario ya existe.");
        }
        Optional<User> existingByEmail = userRepository.findByEmail(payload.getEmail());
        if (existingByEmail.isPresent()) {
            throw new IllegalArgumentException("El correo electronico: " + payload.getEmail() + " ya esta asociado a otro usuario.");
        }


        String rawPassword = payload.getEncryptedPassword();
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("password es obligatorio.");
        }
        String hashed = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
        payload.setEncryptedPassword(hashed);

        // Guardar usuario
        return userRepository.save(payload);
    }
}
