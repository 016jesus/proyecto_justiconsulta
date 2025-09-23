package com.justiconsulta.store.model;


import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "legal_process")
@Data @NoArgsConstructor @AllArgsConstructor
public class LegalProcess {
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_document_number")
    private User user;

    @Column(name = "last_action_date")
    private OffsetDateTime lastActionDate;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

}
