package com.justiconsulta.store.repository;

import com.justiconsulta.store.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByDocumentNumber(String documentNumber);
    Optional<User> findBySupabaseUserId(UUID supabaseUserId);
}
