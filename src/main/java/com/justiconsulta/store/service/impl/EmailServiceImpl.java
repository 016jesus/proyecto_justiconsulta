package com.justiconsulta.store.service.impl;

import com.justiconsulta.store.model.User;
import com.justiconsulta.store.service.EmailService;
import com.justiconsulta.store.service.EmailTemplateService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final EmailTemplateService templateService;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.url}")
    private String appUrl;

    public EmailServiceImpl(JavaMailSender mailSender, EmailTemplateService templateService) {
        this.mailSender = mailSender;
        this.templateService = templateService;
    }

    @Override
    public void sendUserWelcomeEmail(User user) {
        try {
            String userName = user.getFirstName() + " " + user.getFirstLastName();
            String htmlContent = templateService.getWelcomeEmailTemplate(userName, user.getEmail())
                    .replace("${APP_URL}", appUrl);

            sendHtmlEmail(user.getEmail(), "¡Bienvenido a JustiConsulta!", htmlContent);
            logger.info("Welcome email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Error sending welcome email to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    @Override
    public void sendNewActuationEmail(User user, String numeroRadicacion, String actuacion, String fecha) {
        try {
            String userName = user.getFirstName() + " " + user.getFirstLastName();
            String htmlContent = templateService.getNewActuationEmailTemplate(
                    userName, numeroRadicacion, actuacion, fecha)
                    .replace("${APP_URL}", appUrl);

            sendHtmlEmail(user.getEmail(), "Nueva Actuación Procesal - JustiConsulta", htmlContent);
            logger.info("New actuation email sent successfully to: {} for process: {}",
                    user.getEmail(), numeroRadicacion);
        } catch (Exception e) {
            logger.error("Error sending actuation email to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    @Override
    public void sendPasswordResetEmail(User user, String resetLink) {
        try {
            String userName = user.getFirstName() + " " + user.getFirstLastName();
            String htmlContent = templateService.getPasswordResetEmailTemplate(userName, resetLink)
                    .replace("${APP_URL}", appUrl);

            sendHtmlEmail(user.getEmail(), "Recuperación de Contraseña - JustiConsulta", htmlContent);
            logger.info("Password reset email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Error sending password reset email to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    @Override
    public void sendProcessDeletedEmail(User user, String numeroRadicacion) {
        try {
            String userName = user.getFirstName() + " " + user.getFirstLastName();
            String htmlContent = templateService.getProcessDeletedEmailTemplate(userName, numeroRadicacion)
                    .replace("${APP_URL}", appUrl);

            sendHtmlEmail(user.getEmail(), "Proceso Eliminado del Seguimiento - JustiConsulta", htmlContent);
            logger.info("Process deleted email sent successfully to: {} for process: {}",
                    user.getEmail(), numeroRadicacion);
        } catch (Exception e) {
            logger.error("Error sending process deleted email to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    @Override
    public void sendActuationReminderEmail(User user, int cantidadProcesos) {
        try {
            String userName = user.getFirstName() + " " + user.getFirstLastName();
            String htmlContent = templateService.getActuationReminderEmailTemplate(userName, cantidadProcesos)
                    .replace("${APP_URL}", appUrl);

            sendHtmlEmail(user.getEmail(), "Recordatorio de Actuaciones - JustiConsulta", htmlContent);
            logger.info("Actuation reminder email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Error sending reminder email to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    @Override
    public void sendProcessAssociatedEmail(User user, String numeroRadicacion, String despacho) {
        try {
            String userName = user.getFirstName() + " " + user.getFirstLastName();
            String htmlContent = templateService.getProcessAssociatedEmailTemplate(userName, numeroRadicacion, despacho)
                    .replace("${APP_URL}", appUrl);

            sendHtmlEmail(user.getEmail(), "Proceso Agregado a tu Seguimiento - JustiConsulta", htmlContent);
            logger.info("Process associated email sent successfully to: {} for process: {}",
                    user.getEmail(), numeroRadicacion);
        } catch (Exception e) {
            logger.error("Error sending process associated email to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        try {
            helper.setFrom(fromEmail, fromName);
        } catch (Exception e) {
            helper.setFrom(fromEmail);
        }
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true = HTML

        mailSender.send(message);
        logger.debug("HTML email sent to: {} with subject: {}", to, subject);
    }
}
