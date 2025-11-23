package com.justiconsulta.store.service.contract;

import com.justiconsulta.store.model.User;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface IUserService {
    ResponseEntity<User> getUserByDocument(String documentNumber);
    ResponseEntity<User> getUserByEmail(String email);
    ResponseEntity<List<String>> getLegalProcessIdsByUser(String documentNumber);
}

