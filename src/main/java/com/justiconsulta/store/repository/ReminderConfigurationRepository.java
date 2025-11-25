package com.justiconsulta.store.repository;

import com.justiconsulta.store.model.ReminderConfiguration;
import com.justiconsulta.store.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderConfigurationRepository extends JpaRepository<ReminderConfiguration, String> {
    Optional<ReminderConfiguration> findByUser(User user);
    Optional<ReminderConfiguration> findByUserDocumentNumber(String userDocumentNumber);
    List<ReminderConfiguration> findByEnabledTrue();
}

