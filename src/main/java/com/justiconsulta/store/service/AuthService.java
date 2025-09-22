package com.justiconsulta.store.service;

import com.justiconsulta.store.model.User;
import com.justiconsulta.store.repository.UserRepository;
import com.justiconsulta.store.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

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
}
