package com.justiconsulta.store.service.impl;

import com.justiconsulta.store.model.User;
import com.justiconsulta.store.repository.UserLegalProcessRepository;
import com.justiconsulta.store.repository.UserRepository;
import com.justiconsulta.store.service.contract.IUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements IUserService {
    private final UserRepository userRepository;
    private final UserLegalProcessRepository userLegalProcessRepository;

    public UserServiceImpl(UserRepository userRepository, UserLegalProcessRepository userLegalProcessRepository) {
        this.userRepository = userRepository;
        this.userLegalProcessRepository = userLegalProcessRepository;
    }

    @Override
    public ResponseEntity<User> getUserByDocument(String documentNumber) {
        return userRepository.findById(documentNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<List<String>> getLegalProcessIdsByUser(String documentNumber) {
        List<String> processIds = userLegalProcessRepository.findProcessIdsByUserDocumentNumber(documentNumber);
        return ResponseEntity.ok(processIds);
    }
}

