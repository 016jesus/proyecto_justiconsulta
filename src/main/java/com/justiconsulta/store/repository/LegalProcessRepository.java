package com.justiconsulta.store.repository;


import com.justiconsulta.store.model.LegalProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LegalProcessRepository extends JpaRepository<LegalProcess, UUID> {
    @Query("SELECT u.id.legalProcessId FROM UserLegalProcess u WHERE u.id.userDocumentNumber = :documentNumber")
    List<String> findProcessIdsByUserDocumentNumber(@Param("documentNumber") String documentNumber);

    boolean existsById(LegalProcess.LegalProcessId id);

    Optional<LegalProcess> findById(LegalProcess.LegalProcessId legalProcessId);

    List<LegalProcess> findByIdUserDocumentNumber(String userDocumentNumber);
}
