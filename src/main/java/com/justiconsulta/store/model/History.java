package com.justiconsulta.store.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "history")
@Data @NoArgsConstructor @AllArgsConstructor
public class History {
    @Id
    @GeneratedValue(generator = "UUID")
    @org.hibernate.annotations.GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    // legal process id stored as string (se cambió en la BD)
    @Column(name = "legal_process_id")
    private String legalProcessId;

    @Column(name = "activity_series_id", columnDefinition = "uuid")
    private UUID activitySeriesId;

    @Column(name = "date", nullable = false)
    private OffsetDateTime date;

    @Column(name = "result")
    private String result;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    // user_document_number stored as TEXT in DB
    @Column(name = "user_document_number", columnDefinition = "text")
    private String userDocumentNumber;
}
