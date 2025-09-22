package com.justiconsulta.store.repository;

import com.justiconsulta.store.model.ActivitySeries;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ActivitySeriesRepository extends JpaRepository<ActivitySeries, UUID> {
    // Métodos CRUD heredados de JpaRepository
}

