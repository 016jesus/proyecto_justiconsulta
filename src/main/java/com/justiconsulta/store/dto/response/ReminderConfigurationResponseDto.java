package com.justiconsulta.store.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

/**
 * DTO de respuesta para la configuración de recordatorios
 */
public class ReminderConfigurationResponseDto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("frequencyDays")
    private Integer frequencyDays;

    @JsonProperty("reminderHour")
    private Integer reminderHour;

    @JsonProperty("reminderMinute")
    private Integer reminderMinute;

    @JsonProperty("startHour")
    private Integer startHour;

    @JsonProperty("endHour")
    private Integer endHour;

    @JsonProperty("lastReminderSent")
    private OffsetDateTime lastReminderSent;

    @JsonProperty("createdAt")
    private OffsetDateTime createdAt;

    @JsonProperty("updatedAt")
    private OffsetDateTime updatedAt;

    // Constructores
    public ReminderConfigurationResponseDto() {}

    public ReminderConfigurationResponseDto(String id, Boolean enabled, Integer frequencyDays,
                                           Integer reminderHour, Integer reminderMinute,
                                           Integer startHour, Integer endHour,
                                           OffsetDateTime lastReminderSent,
                                           OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.enabled = enabled;
        this.frequencyDays = frequencyDays;
        this.reminderHour = reminderHour;
        this.reminderMinute = reminderMinute;
        this.startHour = startHour;
        this.endHour = endHour;
        this.lastReminderSent = lastReminderSent;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

