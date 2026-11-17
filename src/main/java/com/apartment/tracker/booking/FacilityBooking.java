package com.apartment.tracker.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

@Entity
public class FacilityBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Facility facility;

    @Column(nullable = false, length = 100)
    private String residentUsername;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FacilityBookingStatus status = FacilityBookingStatus.BOOKED;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected FacilityBooking() {
    }

    public FacilityBooking(Facility facility, String residentUsername, LocalDateTime startAt, LocalDateTime endAt) {
        this.facility = facility;
        this.residentUsername = residentUsername;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public Long getId() {
        return id;
    }

    public Facility getFacility() {
        return facility;
    }

    public String getResidentUsername() {
        return residentUsername;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public FacilityBookingStatus getStatus() {
        return status;
    }

    public void cancel() {
        status = FacilityBookingStatus.CANCELLED;
    }
}
