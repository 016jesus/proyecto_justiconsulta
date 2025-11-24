package com.justiconsulta.store.controller;

import com.justiconsulta.store.model.Notification;
import com.justiconsulta.store.model.User;
import com.justiconsulta.store.repository.UserRepository;
import com.justiconsulta.store.service.impl.NotificationServiceImpl;
import com.justiconsulta.store.service.contract.INotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final INotificationService notificationService;
    private final NotificationServiceImpl notificationServiceImpl;
    private final UserRepository userRepository;

    public NotificationController(INotificationService notificationService,
                                  NotificationServiceImpl notificationServiceImpl,
                                  UserRepository userRepository) {
        this.notificationService = notificationService;
        this.notificationServiceImpl = notificationServiceImpl;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Notification> getAllNotifications() {
        return notificationService.getAllNotifications();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notification> getNotification(@PathVariable UUID id) {
        return notificationService.getNotification(id);
    }

    /**
     * Obtener notificaciones del usuario autenticado
     */
    @GetMapping("/my-notifications")
    public ResponseEntity<List<Notification>> getMyNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findById(userDetails.getUsername())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        List<Notification> notifications = notificationServiceImpl.getNotificationsByUser(user);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Obtener notificaciones no leídas del usuario autenticado
     */
    @GetMapping("/my-notifications/unread")
    public ResponseEntity<List<Notification>> getMyUnreadNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findById(userDetails.getUsername())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        List<Notification> notifications = notificationServiceImpl.getUnreadNotificationsByUser(user);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Contar notificaciones no leídas
     */
    @GetMapping("/my-notifications/unread/count")
    public ResponseEntity<Long> countUnreadNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findById(userDetails.getUsername())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        long count = notificationServiceImpl.countUnreadNotifications(user);
        return ResponseEntity.ok(count);
    }

    /**
     * Marcar notificación como leída
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable UUID id) {
        return notificationServiceImpl.markAsRead(id);
    }

    /**
     * Marcar todas las notificaciones como leídas
     */
    @PutMapping("/mark-all-read")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findById(userDetails.getUsername())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        notificationServiceImpl.markAllAsRead(user);
        return ResponseEntity.ok().build();
    }

    /**
     * Eliminar notificación
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable UUID id) {
        return notificationServiceImpl.deleteNotification(id);
    }
}
