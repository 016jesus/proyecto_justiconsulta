package com.justiconsulta.store.repository;

import com.justiconsulta.store.model.UserLegalProcess;
import com.justiconsulta.store.model.UserLegalProcess.UserLegalProcessId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserLegalProcessRepository extends JpaRepository<UserLegalProcess, UserLegalProcessId> {
    @Query("SELECT u.id.legalProcessId FROM UserLegalProcess u WHERE u.id.userDocumentNumber = :documentNumber")
    List<String> findProcessIdsByUserDocumentNumber(@Param("documentNumber") String documentNumber);


}
