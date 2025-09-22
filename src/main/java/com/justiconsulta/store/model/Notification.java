package com.justiconsulta.store.model;


import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification")
@Data @NoArgsConstructor @AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(generator = "UUID")
    @org.hibernate.annotations.GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "notification_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID notificationId;

    @ManyToOne
    @JoinColumn(name = "user_document_number")
    private User user;

    @ManyToOne
    @JoinColumn(name = "action_id")
    private Action action;

    @Column(name = "type")
    private String type;

    @Column(name = "message")
    private String message;

    @Column(name = "date")
    private OffsetDateTime date;

    @Column(name = "is_read")
    private Boolean isRead;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
