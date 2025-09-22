package com.justiconsulta.store.repository;



import com.justiconsulta.store.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByNotificationIdAfterOrderByDate(UUID notificationId);
}
