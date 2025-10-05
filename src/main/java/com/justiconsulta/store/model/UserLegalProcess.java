package com.justiconsulta.store.model;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_legal_processes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLegalProcess {
    @EmbeddedId
    private UserLegalProcessId id;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserLegalProcessId implements Serializable {
        @Column(name = "user_document_number", length = 50)
        private String userDocumentNumber;
        @Column(name = "legal_process_id")
        private String legalProcessId;
    }
}
