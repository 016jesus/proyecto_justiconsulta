package com.justiconsulta.store.service;

import com.justiconsulta.store.model.User;

public interface EmailService {
    void sendUserWelcomeEmail(User user);
}
