package com.justiconsulta.store.repository;


import com.justiconsulta.store.model.Action;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ActionRepository extends JpaRepository<Action, UUID> {
    List<Action> findByIdOrderByDate(UUID id);
}
