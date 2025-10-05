package com.justiconsulta.store.model;


import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "legal_process")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LegalProcess {

    @EmbeddedId
    private LegalProcessId id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_document_number", referencedColumnName = "document_number", nullable = false, insertable = false, updatable = false)
    private User user;

    @Column(name = "last_action_date")
    private OffsetDateTime lastActionDate;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LegalProcessId implements Serializable {

        @Column(name = "id", updatable = false, nullable = false)
        private String id;

        @Column(name = "user_document_number", length = 50, nullable = false)
        private String userDocumentNumber;
    }
}
