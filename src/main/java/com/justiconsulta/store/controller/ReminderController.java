package com.justiconsulta.store.controller;

import com.justiconsulta.store.dto.request.ReminderConfigurationUpdateDto;
import com.justiconsulta.store.model.ReminderConfiguration;
import com.justiconsulta.store.model.User;
import com.justiconsulta.store.repository.UserRepository;
import com.justiconsulta.store.service.contract.IReminderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Controlador para gestionar la configuración de recordatorios automáticos
 */
@RestController
@RequestMapping("/api/test/notifications/reminder")
public class ReminderController {

    private final IReminderService reminderService;
    private final UserRepository userRepository;

    public ReminderController(IReminderService reminderService, UserRepository userRepository) {
        this.reminderService = reminderService;
        this.userRepository = userRepository;
    }

    /**
     * Obtener la configuración actual de recordatorios del usuario autenticado
     * GET /api/test/notifications/reminder
     */
    @GetMapping
    public ResponseEntity<?> getReminderConfiguration() {
        Optional<String> userDocumentNumber = resolveUserDocumentNumber();
        if (userDocumentNumber.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autenticado o no se pudo resolver el usuario"));
        }

        Optional<User> userOpt = userRepository.findByDocumentNumber(userDocumentNumber.get());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado"));
        }

        ReminderConfiguration config = reminderService.getReminderConfig(userOpt.get());
        return ResponseEntity.ok(config);
    }

    /**
     * Actualizar la configuración de recordatorios del usuario
     * PUT /api/test/notifications/reminder
     * Body: {
     *   "enabled": true,
     *   "frequencyDays": 7,
     *   "reminderHour": 9,
     *   "reminderMinute": 0,
     *   "startHour": 7,
     *   "endHour": 22
     * }
     */
    @PutMapping
    public ResponseEntity<?> updateReminderConfiguration(
            @RequestBody ReminderConfigurationUpdateDto updateDto) {
        Optional<String> userDocumentNumber = resolveUserDocumentNumber();
        if (userDocumentNumber.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autenticado o no se pudo resolver el usuario"));
        }

        Optional<User> userOpt = userRepository.findByDocumentNumber(userDocumentNumber.get());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado"));
        }

        try {
            // Convertir DTO a modelo
            ReminderConfiguration updateModel = new ReminderConfiguration();
            updateModel.setEnabled(updateDto.getEnabled());
            updateModel.setFrequencyDays(updateDto.getFrequencyDays());
            updateModel.setReminderHour(updateDto.getReminderHour());
            updateModel.setReminderMinute(updateDto.getReminderMinute());
            updateModel.setStartHour(updateDto.getStartHour());
            updateModel.setEndHour(updateDto.getEndHour());

            ReminderConfiguration updated = reminderService.updateReminderConfig(userOpt.get(), updateModel);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Configuración de recordatorios actualizada correctamente",
                    "configuration", updated
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al actualizar la configuración: " + e.getMessage()));
        }
    }

    /**
     * Habilitar o deshabilitar los recordatorios
     * POST /api/test/notifications/reminder/toggle
     * Body: { "enabled": true }
     */
    @PostMapping("/toggle")
    public ResponseEntity<?> toggleReminders(@RequestBody Map<String, Boolean> request) {
        Optional<String> userDocumentNumber = resolveUserDocumentNumber();
        if (userDocumentNumber.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autenticado o no se pudo resolver el usuario"));
        }

        Optional<User> userOpt = userRepository.findByDocumentNumber(userDocumentNumber.get());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado"));
        }

        Boolean enabled = request.getOrDefault("enabled", true);

        try {
            ReminderConfiguration updated = reminderService.toggleReminders(userOpt.get(), enabled);
            String message = enabled ? "Recordatorios habilitados" : "Recordatorios deshabilitados";
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", message,
                    "configuration", updated
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al cambiar estado de recordatorios: " + e.getMessage()));
        }
    }

    /**
     * Obtener los presets recomendados de configuración
     * GET /api/test/notifications/reminder/presets
     */
    @GetMapping("/presets")
    public ResponseEntity<?> getReminderPresets() {
        Map<String, Object> presets = Map.of(
                "daily", Map.of(
                        "name", "Diariamente",
                        "frequencyDays", 1,
                        "reminderHour", 9,
                        "reminderMinute", 0,
                        "description", "Recibe un recordatorio cada día a las 9:00 AM"
                ),
                "weekly", Map.of(
                        "name", "Semanalmente",
                        "frequencyDays", 7,
                        "reminderHour", 9,
                        "reminderMinute", 0,
                        "description", "Recibe un recordatorio cada semana los lunes a las 9:00 AM"
                ),
                "biweekly", Map.of(
                        "name", "Cada dos semanas",
                        "frequencyDays", 14,
                        "reminderHour", 9,
                        "reminderMinute", 0,
                        "description", "Recibe un recordatorio cada dos semanas a las 9:00 AM"
                ),
                "monthly", Map.of(
                        "name", "Mensualmente",
                        "frequencyDays", 30,
                        "reminderHour", 9,
                        "reminderMinute", 0,
                        "description", "Recibe un recordatorio cada mes a las 9:00 AM"
                )
        );

        return ResponseEntity.ok(Map.of(
                "presets", presets,
                "note", "Puedes usar estos presets como base y ajustar los valores según tus necesidades"
        ));
    }

    /**
     * Configurar recordatorios con un preset
     * POST /api/test/notifications/reminder/preset/{presetName}
     * Preset names: daily, weekly, biweekly, monthly
     */
    @PostMapping("/preset/{presetName}")
    public ResponseEntity<?> applyPreset(@PathVariable String presetName) {
        Optional<String> userDocumentNumber = resolveUserDocumentNumber();
        if (userDocumentNumber.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autenticado o no se pudo resolver el usuario"));
        }

        Optional<User> userOpt = userRepository.findByDocumentNumber(userDocumentNumber.get());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado"));
        }

        ReminderConfiguration updateModel = new ReminderConfiguration();

        switch (presetName.toLowerCase()) {
            case "daily":
                updateModel.setFrequencyDays(1);
                break;
            case "weekly":
                updateModel.setFrequencyDays(7);
                break;
            case "biweekly":
                updateModel.setFrequencyDays(14);
                break;
            case "monthly":
                updateModel.setFrequencyDays(30);
                break;
            default:
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Preset no válido. Use: daily, weekly, biweekly, monthly"));
        }

        updateModel.setReminderHour(9);
        updateModel.setReminderMinute(0);
        updateModel.setEnabled(true);

        try {
            ReminderConfiguration updated = reminderService.updateReminderConfig(userOpt.get(), updateModel);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Preset '" + presetName + "' aplicado correctamente",
                    "configuration", updated
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al aplicar preset: " + e.getMessage()));
        }
    }

    /**
     * Resolver el documento del usuario desde el contexto de autenticación
     */
    private Optional<String> resolveUserDocumentNumber() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return Optional.empty();
        }

        String principal = auth.getName();
        try {
            // Intenta convertir a UUID (Supabase ID)
            UUID supabaseId = UUID.fromString(principal);
            return userRepository.findBySupabaseUserId(supabaseId)
                    .map(User::getDocumentNumber);
        } catch (IllegalArgumentException e) {
            // Si no es UUID, devolver como documentNumber
            return Optional.of(principal);
        }
    }
}

