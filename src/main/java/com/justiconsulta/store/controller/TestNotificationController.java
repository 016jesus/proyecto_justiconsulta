package com.justiconsulta.store.controller;

import com.justiconsulta.store.model.User;
import com.justiconsulta.store.repository.UserRepository;
import com.justiconsulta.store.service.impl.NotificationServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador para enviar notificaciones de prueba
 * Útil para probar el servicio de correos Gmail SMTP
 */
@RestController
@RequestMapping("/api/test/notifications")
public class TestNotificationController {

    private final NotificationServiceImpl notificationService;
    private final UserRepository userRepository;

    public TestNotificationController(NotificationServiceImpl notificationService,
                                     UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    /**
     * Enviar correo de bienvenida de prueba
     * POST /api/test/notifications/welcome
     * Body: { "userDocumentNumber": "123456789" }
     */
    @PostMapping("/welcome")
    public ResponseEntity<?> sendWelcomeNotification(@RequestBody Map<String, String> request) {
        String documentNumber = request.get("userDocumentNumber");

        User user = userRepository.findById(documentNumber)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Usuario no encontrado con documento: " + documentNumber));
        }

        try {
            notificationService.sendWelcomeNotification(user);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Correo de bienvenida enviado a: " + user.getEmail(),
                    "user", user.getFirstName() + " " + user.getFirstLastName()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al enviar correo: " + e.getMessage()));
        }
    }

    /**
     * Enviar notificación de nueva actuación de prueba
     * POST /api/test/notifications/new-actuation
     * Body: {
     *   "userDocumentNumber": "123456789",
     *   "numeroRadicacion": "50001333100120070007600",
     *   "actuacion": "Se admite la demanda",
     *   "fecha": "2024-11-23"
     * }
     */
    @PostMapping("/new-actuation")
    public ResponseEntity<?> sendNewActuationNotification(@RequestBody Map<String, String> request) {
        String documentNumber = request.get("userDocumentNumber");
        String numeroRadicacion = request.get("numeroRadicacion");
        String actuacion = request.get("actuacion");
        String fecha = request.get("fecha");

        if (numeroRadicacion == null || actuacion == null || fecha == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Faltan campos requeridos: numeroRadicacion, actuacion, fecha"));
        }

        User user = userRepository.findById(documentNumber)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Usuario no encontrado con documento: " + documentNumber));
        }

        try {
            notificationService.sendNewActuationNotification(user, numeroRadicacion, actuacion, fecha);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Notificación de nueva actuación enviada a: " + user.getEmail(),
                    "numeroRadicacion", numeroRadicacion
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al enviar notificación: " + e.getMessage()));
        }
    }

    /**
     * Enviar notificación de proceso eliminado de prueba
     * POST /api/test/notifications/process-deleted
     * Body: {
     *   "userDocumentNumber": "123456789",
     *   "numeroRadicacion": "50001333100120070007600"
     * }
     */
    @PostMapping("/process-deleted")
    public ResponseEntity<?> sendProcessDeletedNotification(@RequestBody Map<String, String> request) {
        String documentNumber = request.get("userDocumentNumber");
        String numeroRadicacion = request.get("numeroRadicacion");

        if (numeroRadicacion == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Falta el campo requerido: numeroRadicacion"));
        }

        User user = userRepository.findById(documentNumber)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Usuario no encontrado con documento: " + documentNumber));
        }

        try {
            notificationService.sendProcessDeletedNotification(user, numeroRadicacion);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Notificación de proceso eliminado enviada a: " + user.getEmail(),
                    "numeroRadicacion", numeroRadicacion
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al enviar notificación: " + e.getMessage()));
        }
    }

    /**
     * Enviar recordatorio de actuaciones de prueba
     * POST /api/test/notifications/reminder
     * Body: {
     *   "userDocumentNumber": "123456789",
     *   "cantidadProcesos": 5
     * }
     */
    @PostMapping("/reminder")
    public ResponseEntity<?> sendActuationReminderNotification(@RequestBody Map<String, Object> request) {
        String documentNumber = (String) request.get("userDocumentNumber");
        Integer cantidadProcesos = request.get("cantidadProcesos") != null
                ? Integer.parseInt(request.get("cantidadProcesos").toString())
                : 1;

        User user = userRepository.findById(documentNumber)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Usuario no encontrado con documento: " + documentNumber));
        }

        try {
            notificationService.sendActuationReminderNotification(user, cantidadProcesos);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Recordatorio enviado a: " + user.getEmail(),
                    "cantidadProcesos", cantidadProcesos
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al enviar recordatorio: " + e.getMessage()));
        }
    }

    /**
     * Endpoint de información sobre cómo probar las notificaciones
     */
    @GetMapping("/info")
    public ResponseEntity<?> getTestInfo() {
        return ResponseEntity.ok(Map.of(
                "message", "Endpoints de prueba para notificaciones por correo",
                "smtp", "Gmail SMTP configurado",
                "endpoints", Map.of(
                        "POST /api/test/notifications/welcome", "Enviar correo de bienvenida",
                        "POST /api/test/notifications/new-actuation", "Enviar notificación de nueva actuación",
                        "POST /api/test/notifications/process-deleted", "Enviar notificación de proceso eliminado",
                        "POST /api/test/notifications/reminder", "Enviar recordatorio de actuaciones"
                ),
                "configuracion", Map.of(
                        "nota", "Asegúrate de configurar las variables de entorno GMAIL_USERNAME y GMAIL_APP_PASSWORD",
                        "appPassword", "Genera una contraseña de aplicación en: https://myaccount.google.com/apppasswords"
                )
        ));
    }
}

