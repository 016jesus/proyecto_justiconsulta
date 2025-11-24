package com.justiconsulta.store.service.impl;

import com.justiconsulta.store.model.Notification;
import com.justiconsulta.store.model.User;
import com.justiconsulta.store.repository.NotificationRepository;
import com.justiconsulta.store.service.EmailService;
import com.justiconsulta.store.service.contract.INotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
    }

    @Override
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    @Override
    public ResponseEntity<Notification> getNotification(UUID id) {
        return notificationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Crea y envía una notificación de bienvenida
     */
    public Notification sendWelcomeNotification(User user) {
        Notification notification = createNotification(
                user,
                "WELCOME",
                "¡Bienvenido a JustiConsulta! Tu cuenta ha sido creada exitosamente."
        );

        // Enviar correo de bienvenida
        emailService.sendUserWelcomeEmail(user);

        return notificationRepository.save(notification);
    }

    /**
     * Crea y envía una notificación de nueva actuación procesal
     */
    public Notification sendNewActuationNotification(User user, String numeroRadicacion,
                                                     String actuacion, String fecha) {
        String message = String.format(
                "Nueva actuación en el proceso %s registrada el %s",
                numeroRadicacion, fecha
        );

        Notification notification = createNotification(
                user,
                "NEW_ACTUATION",
                message
        );

        // Enviar correo con la nueva actuación
        emailService.sendNewActuationEmail(user, numeroRadicacion, actuacion, fecha);

        return notificationRepository.save(notification);
    }

    /**
     * Crea y envía una notificación de proceso eliminado
     */
    public Notification sendProcessDeletedNotification(User user, String numeroRadicacion) {
        String message = String.format(
                "El proceso %s ha sido eliminado de tu lista de seguimiento",
                numeroRadicacion
        );

        Notification notification = createNotification(
                user,
                "PROCESS_DELETED",
                message
        );

        // Enviar correo de confirmación
        emailService.sendProcessDeletedEmail(user, numeroRadicacion);

        return notificationRepository.save(notification);
    }

    /**
     * Crea y envía un recordatorio de actuaciones pendientes
     */
    public Notification sendActuationReminderNotification(User user, int cantidadProcesos) {
        String message = String.format(
                "Tienes %d proceso(s) en seguimiento. Revisa su estado.",
                cantidadProcesos
        );

        Notification notification = createNotification(
                user,
                "REMINDER",
                message
        );

        // Enviar correo de recordatorio
        emailService.sendActuationReminderEmail(user, cantidadProcesos);

        return notificationRepository.save(notification);
    }

    /**
     * Crea y envía una notificación de proceso asociado exitosamente
     */
    public Notification sendProcessAssociatedNotification(User user, String numeroRadicacion, String despacho) {
        String message = String.format(
                "El proceso %s ha sido agregado a tu lista de seguimiento",
                numeroRadicacion
        );

        Notification notification = createNotification(
                user,
                "PROCESS_ASSOCIATED",
                message
        );

        // Enviar correo de confirmación
        emailService.sendProcessAssociatedEmail(user, numeroRadicacion, despacho);

        return notificationRepository.save(notification);
    }

    /**
     * Obtiene todas las notificaciones de un usuario específico
     */
    public List<Notification> getNotificationsByUser(User user) {
        return notificationRepository.findByUserOrderByDateDesc(user);
    }

    /**
     * Obtiene las notificaciones no leídas de un usuario
     */
    public List<Notification> getUnreadNotificationsByUser(User user) {
        return notificationRepository.findByUserAndIsReadFalseOrderByDateDesc(user);
    }

    /**
     * Marca una notificación como leída
     */
    public ResponseEntity<Notification> markAsRead(UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .map(notification -> {
                    notification.setIsRead(true);
                    Notification updated = notificationRepository.save(notification);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Marca todas las notificaciones de un usuario como leídas
     */
    public void markAllAsRead(User user) {
        List<Notification> unreadNotifications = getUnreadNotificationsByUser(user);
        unreadNotifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    /**
     * Elimina una notificación
     */
    public ResponseEntity<Void> deleteNotification(UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .map(notification -> {
                    notificationRepository.delete(notification);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cuenta las notificaciones no leídas de un usuario
     */
    public long countUnreadNotifications(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    /**
     * Método auxiliar para crear una notificación
     */
    private Notification createNotification(User user, String type, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setMessage(message);
        notification.setDate(OffsetDateTime.now());
        notification.setIsRead(false);
        notification.setCreatedAt(OffsetDateTime.now());
        return notification;
    }
}
