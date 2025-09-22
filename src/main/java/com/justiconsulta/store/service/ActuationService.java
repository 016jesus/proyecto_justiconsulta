package com.justiconsulta.store.service;

import com.justiconsulta.store.model.Action;
import com.justiconsulta.store.model.Notification;
import com.justiconsulta.store.model.LegalProcess;
import com.justiconsulta.store.model.User;
import com.justiconsulta.store.repository.ActionRepository;
import com.justiconsulta.store.repository.NotificationRepository;
import com.justiconsulta.store.repository.LegalProcessRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ActuationService {

    private final ActionRepository actionRepo;
    private final NotificationRepository notificationRepo;
    private final LegalProcessRepository processRepo;

    public ActuationService(ActionRepository actionRepo,
                            NotificationRepository notificationRepo,
                            LegalProcessRepository processRepo) {
        this.actionRepo = actionRepo;
        this.notificationRepo = notificationRepo;
        this.processRepo = processRepo;
    }

    @Transactional
    public Action createActionAndNotify(UUID legalProcessId, String description) {
        OffsetDateTime now = OffsetDateTime.now();

        LegalProcess process = processRepo.findById(legalProcessId)
                .orElseThrow(() -> new IllegalArgumentException("LegalProcess does not exist: " + legalProcessId));
        User user = process.getUser();

        Action action = new Action();
        // Si tienes ActivitySeries, asígnala aquí si corresponde
        action.setDescription(description);
        action.setDate(now);
        action.setCreatedAt(now);
        action = actionRepo.save(action);

        process.setLastActionDate(now);
        processRepo.save(process);

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setAction(action);
        notification.setType("ACTION_NEW");
        notification.setMessage("A new action was added: " + (description == null ? "" : description));
        notification.setDate(now);
        notification.setIsRead(false);
        notification.setCreatedAt(now);
        notificationRepo.save(notification);

        return action;
    }
}
