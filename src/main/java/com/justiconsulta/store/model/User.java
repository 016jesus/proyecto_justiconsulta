package com.justiconsulta.store.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "\"user\"")
@Data @NoArgsConstructor @AllArgsConstructor
public class User {
    @Id
    @Column(name = "document_number", length = 50)
    private String documentNumber;

    @Column(name = "document_type", length = 50, nullable = false)
    private String documentType;

    @Column(name = "first_name", length = 120, nullable = false)
    private String firstName;

    @Column(name = "middle_name", length = 120)
    private String middleName;

    @Column(name = "last_name", length = 120, nullable = false)
    private String lastName;

    @Column(name = "second_last_name", length = 120)
    private String secondLastName;

    @Column(name = "email", length = 255, unique = true, nullable = false)
    private String email;

    @JsonIgnore
    @Column(name = "encrypted_password", nullable = false)
    private String encryptedPassword;

    @Column(name = "birth_date")
    private java.sql.Date birthDate;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
