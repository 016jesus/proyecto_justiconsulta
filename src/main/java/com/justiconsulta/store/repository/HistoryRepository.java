package com.justiconsulta.store.repository;

import com.justiconsulta.store.model.History;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface HistoryRepository extends JpaRepository<History, UUID> {


    List<History> findByUserDocumentNumberOrderByDateDesc(String userDocumentNumber);
}
