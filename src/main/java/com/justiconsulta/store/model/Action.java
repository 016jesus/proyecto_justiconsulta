package com.justiconsulta.store.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "action")
@Data @NoArgsConstructor @AllArgsConstructor
public class Action {
    @Id
    @GeneratedValue(generator = "UUID")
    @org.hibernate.annotations.GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "activity_series_id")
    private ActivitySeries activitySeries;

    @Column(name = "description")
    private String description;

    @Column(name = "date", nullable = false)
    private OffsetDateTime date;

    @Column(name = "linked_document_id", columnDefinition = "uuid")
    private UUID linkedDocumentId;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }
    public ActivitySeries getActivitySeries() {
        return activitySeries;
    }
    public void setActivitySeries(ActivitySeries activitySeries) {
        this.activitySeries = activitySeries;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public OffsetDateTime getDate() {
        return date;
    }
    public void setDate(OffsetDateTime date) {
        this.date = date;
    }
    public UUID getLinkedDocumentId() {
        return linkedDocumentId;
    }
    public void setLinkedDocumentId(UUID linkedDocumentId) {
        this.linkedDocumentId = linkedDocumentId;
    }
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }


}
