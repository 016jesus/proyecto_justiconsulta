package com.justiconsulta.store.service.contract;

import com.justiconsulta.store.model.ReminderConfiguration;
import com.justiconsulta.store.model.User;

public interface IReminderService {
    ReminderConfiguration getOrCreateReminderConfig(User user);
    ReminderConfiguration updateReminderConfig(User user, ReminderConfiguration updatedConfig);
    ReminderConfiguration getReminderConfig(User user);
    ReminderConfiguration toggleReminders(User user, boolean enabled);
}

