package com.justiconsulta.store.service.contract;

import com.justiconsulta.store.model.Notification;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public interface INotificationService {
    List<Notification> getAllNotifications();
    ResponseEntity<Notification> getNotification(UUID id);
}
