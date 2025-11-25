package com.justiconsulta.store.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Configuración de recordatorios automáticos de procesos para cada usuario
 */
@Entity
@Table(name = "reminder_configuration")
public class ReminderConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_document_number", nullable = false)
    private User user;

    /**
     * Indica si los recordatorios están habilitados
     */
    @Column(nullable = false)
    private Boolean enabled = true;

    /**
     * Frecuencia en días (ej: 1 = diario, 7 = semanal)
     */
    @Column(nullable = false)
    private Integer frequencyDays = 7;

    /**
     * Hora del día para enviar el recordatorio (0-23)
     */
    @Column(nullable = false)
    private Integer reminderHour = 9;

    /**
     * Minuto de la hora para enviar el recordatorio (0-59)
     */
    @Column(nullable = false)
    private Integer reminderMinute = 0;

    /**
     * Horas de inicio del rango horario permitido (ej: 7 AM)
     */
    @Column(nullable = false)
    private Integer startHour = 7;

    /**
     * Horas de fin del rango horario permitido (ej: 22 PM)
     */
    @Column(nullable = false)
    private Integer endHour = 22;

    /**
     * Fecha de la última vez que se envió un recordatorio
     */
    @Column(name = "last_reminder_sent")
    private OffsetDateTime lastReminderSent;

    /**
     * Fecha de creación de la configuración
     */
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    /**
     * Fecha de última actualización
     */
    @Column(nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // Constructores
    public ReminderConfiguration() {}

    public ReminderConfiguration(User user) {
        this.user = user;
        this.enabled = true;
        this.frequencyDays = 7;
        this.reminderHour = 9;
        this.reminderMinute = 0;
        this.startHour = 7;
        this.endHour = 22;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getFrequencyDays() {
        return frequencyDays;
    }

    public void setFrequencyDays(Integer frequencyDays) {
        this.frequencyDays = frequencyDays;
    }

    public Integer getReminderHour() {
        return reminderHour;
    }

    public void setReminderHour(Integer reminderHour) {
        this.reminderHour = reminderHour;
    }

    public Integer getReminderMinute() {
        return reminderMinute;
    }

    public void setReminderMinute(Integer reminderMinute) {
        this.reminderMinute = reminderMinute;
    }

    public Integer getStartHour() {
        return startHour;
    }

    public void setStartHour(Integer startHour) {
        this.startHour = startHour;
    }

    public Integer getEndHour() {
        return endHour;
    }

    public void setEndHour(Integer endHour) {
        this.endHour = endHour;
    }

    public OffsetDateTime getLastReminderSent() {
        return lastReminderSent;
    }

    public void setLastReminderSent(OffsetDateTime lastReminderSent) {
        this.lastReminderSent = lastReminderSent;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

