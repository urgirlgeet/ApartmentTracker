package com.apartment.tracker.complaint;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

@Entity
public class ComplaintHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Complaint complaint;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, length = 100)
    private String actorUsername;

    @Column(nullable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();

    protected ComplaintHistory() {
    }

    public ComplaintHistory(Complaint complaint, String description, String actorUsername) {
        this.complaint = complaint;
        this.description = description;
        this.actorUsername = actorUsername;
    }

    public String getDescription() {
        return description;
    }

    public Long getComplaintId() {
        return complaint.getId();
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
