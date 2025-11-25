package com.justiconsulta.store.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para actualizar la configuración de recordatorios
 */
public class ReminderConfigurationUpdateDto {

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

    // Constructores
    public ReminderConfigurationUpdateDto() {}

    public ReminderConfigurationUpdateDto(Boolean enabled, Integer frequencyDays,
                                          Integer reminderHour, Integer reminderMinute,
                                          Integer startHour, Integer endHour) {
        this.enabled = enabled;
        this.frequencyDays = frequencyDays;
        this.reminderHour = reminderHour;
        this.reminderMinute = reminderMinute;
        this.startHour = startHour;
        this.endHour = endHour;
    }

    // Getters y Setters
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
}

