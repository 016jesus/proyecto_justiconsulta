package com.justiconsulta.store.repository;

import com.justiconsulta.store.model.History;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface HistoryRepository extends JpaRepository<History, UUID> {
    // Métodos CRUD heredados de JpaRepository
}

