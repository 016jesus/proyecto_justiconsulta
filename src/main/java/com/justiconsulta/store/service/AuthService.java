package com.justiconsulta.store.service;

import com.justiconsulta.store.dto.request.RegisterRequestDTO;
import com.justiconsulta.store.model.User;
import com.justiconsulta.store.repository.UserRepository;
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
        newUser.setFirstLastName(payload.firstLastName()); // <-- ¡Corregido!
        newUser.setSecondLastName(payload.secondLastName());
        newUser.setEmail(payload.email());
        newUser.setEncryptedPassword(hashed);

        return userRepository.save(newUser);
    }
}
