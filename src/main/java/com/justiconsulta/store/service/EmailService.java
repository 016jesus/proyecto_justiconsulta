package com.justiconsulta.store.service;

import com.justiconsulta.store.model.User;
import jakarta.mail.MessagingException;

public interface EmailService {
    void sendUserWelcomeEmail(User user);
    void sendNewActuationEmail(User user, String numeroRadicacion, String actuacion, String fecha);
    void sendPasswordResetEmail(User user, String resetLink);
    void sendProcessDeletedEmail(User user, String numeroRadicacion);
    void sendActuationReminderEmail(User user, int cantidadProcesos);
    void sendProcessAssociatedEmail(User user, String numeroRadicacion, String despacho);
    void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException;
}
