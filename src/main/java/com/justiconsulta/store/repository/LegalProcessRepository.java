package com.justiconsulta.store.repository;


import com.justiconsulta.store.model.LegalProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface LegalProcessRepository extends JpaRepository<LegalProcess, UUID> {}
