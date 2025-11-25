package com.justiconsulta.store.service;

import com.justiconsulta.store.model.ReminderConfiguration;
import com.justiconsulta.store.model.User;
import com.justiconsulta.store.model.UserLegalProcess;
import com.justiconsulta.store.repository.ReminderConfigurationRepository;
import com.justiconsulta.store.repository.UserLegalProcessRepository;
import com.justiconsulta.store.service.contract.IReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Servicio para gestionar recordatorios automáticos de procesos
 */
@Service
@EnableScheduling
public class ReminderService implements IReminderService {
    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final ReminderConfigurationRepository reminderConfigRepository;
    private final UserLegalProcessRepository userLegalProcessRepository;
    private final EmailService emailService;

    public ReminderService(ReminderConfigurationRepository reminderConfigRepository,
                           UserLegalProcessRepository userLegalProcessRepository,
                           EmailService emailService) {
        this.reminderConfigRepository = reminderConfigRepository;
        this.userLegalProcessRepository = userLegalProcessRepository;
        this.emailService = emailService;
    }

    /**
     * Tarea programada que se ejecuta cada hora para verificar y enviar recordatorios
     * Se ejecuta cada hora en el minuto 0
     */
    @Scheduled(cron = "0 0 * * * *") // Cada hora en el minuto 0
    public void processReminders() {
        log.info("Iniciando proceso de recordatorios automáticos...");

        try {
            // Obtener todas las configuraciones de recordatorios habilitadas
            List<ReminderConfiguration> enabledConfigs = reminderConfigRepository.findByEnabledTrue();

            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Bogota")); // Zona horaria de Colombia
            int currentHour = now.getHour();
            int currentMinute = now.getMinute();

            for (ReminderConfiguration config : enabledConfigs) {
                // Verificar si la hora actual coincide con la hora de recordatorio configurada
                if (shouldSendReminder(config, currentHour, currentMinute, now)) {
                    sendReminderToUser(config);
                }
            }

            log.info("Proceso de recordatorios completado exitosamente");
        } catch (Exception e) {
            log.error("Error en el proceso de recordatorios automáticos: {}", e.getMessage(), e);
        }
    }

    /**
     * Determina si se debe enviar un recordatorio basado en la configuración
     */
    private boolean shouldSendReminder(ReminderConfiguration config, int currentHour,
                                      int currentMinute, ZonedDateTime now) {
        // Verificar si está dentro del rango horario permitido
        if (currentHour < config.getStartHour() || currentHour >= config.getEndHour()) {
            return false;
        }

        // Verificar si es la hora configurada
        if (currentHour != config.getReminderHour() || currentMinute != config.getReminderMinute()) {
            return false;
        }

        // Verificar si ya se envió un recordatorio hoy
        OffsetDateTime lastSent = config.getLastReminderSent();
        if (lastSent != null) {
            ZonedDateTime lastSentZoned = lastSent.atZoneSameInstant(ZoneId.of("America/Bogota"));
            ZonedDateTime startOfToday = now.toLocalDate().atStartOfDay(ZoneId.of("America/Bogota"));

            if (lastSentZoned.isAfter(startOfToday)) {
                return false; // Ya fue enviado hoy
            }
        }

        // Verificar la frecuencia (en días)
        if (lastSent != null) {
            OffsetDateTime nextReminderDate = lastSent.plusDays(config.getFrequencyDays());
            return !OffsetDateTime.now().isBefore(nextReminderDate);
        }

        return true;
    }

    /**
     * Envía el recordatorio a un usuario específico
     */
    private void sendReminderToUser(ReminderConfiguration config) {
        try {
            User user = config.getUser();

            // Obtener cantidad de procesos activos del usuario
            List<UserLegalProcess> activeProcesses = userLegalProcessRepository
                    .findByUserDocumentNumber(user.getDocumentNumber());

            int processCount = activeProcesses.size();

            if (processCount > 0) {
                // Enviar correo de recordatorio
                emailService.sendActuationReminderEmail(user, processCount);

                // Actualizar la fecha del último recordatorio enviado
                config.setLastReminderSent(OffsetDateTime.now());
                reminderConfigRepository.save(config);

                log.info("Recordatorio enviado a usuario: {}", user.getEmail());
            } else {
                log.debug("Usuario {} no tiene procesos activos para recordatorio", user.getEmail());
            }
        } catch (Exception e) {
            log.error("Error al enviar recordatorio a usuario: {}", e.getMessage(), e);
        }
    }

    /**
     * Obtiene o crea la configuración de recordatorios para un usuario
     */
    public ReminderConfiguration getOrCreateReminderConfig(User user) {
        return reminderConfigRepository.findByUser(user)
                .orElseGet(() -> {
                    ReminderConfiguration newConfig = new ReminderConfiguration(user);
                    return reminderConfigRepository.save(newConfig);
                });
    }

    /**
     * Actualiza la configuración de recordatorios de un usuario
     */
    public ReminderConfiguration updateReminderConfig(User user, ReminderConfiguration updatedConfig) {
        ReminderConfiguration config = getOrCreateReminderConfig(user);

        if (updatedConfig.getEnabled() != null) {
            config.setEnabled(updatedConfig.getEnabled());
        }
        if (updatedConfig.getFrequencyDays() != null) {
            config.setFrequencyDays(updatedConfig.getFrequencyDays());
        }
        if (updatedConfig.getReminderHour() != null) {
            config.setReminderHour(updatedConfig.getReminderHour());
        }
        if (updatedConfig.getReminderMinute() != null) {
            config.setReminderMinute(updatedConfig.getReminderMinute());
        }
        if (updatedConfig.getStartHour() != null) {
            config.setStartHour(updatedConfig.getStartHour());
        }
        if (updatedConfig.getEndHour() != null) {
            config.setEndHour(updatedConfig.getEndHour());
        }

        config.setUpdatedAt(OffsetDateTime.now());
        return reminderConfigRepository.save(config);
    }

    /**
     * Obtiene la configuración de recordatorios de un usuario
     */
    public ReminderConfiguration getReminderConfig(User user) {
        return getOrCreateReminderConfig(user);
    }

    /**
     * Habilita o deshabilita los recordatorios para un usuario
     */
    public ReminderConfiguration toggleReminders(User user, boolean enabled) {
        ReminderConfiguration config = getOrCreateReminderConfig(user);
        config.setEnabled(enabled);
        config.setUpdatedAt(OffsetDateTime.now());
        return reminderConfigRepository.save(config);
    }
}

