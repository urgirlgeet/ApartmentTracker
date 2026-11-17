package com.apartment.tracker.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class BillPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Bill bill;

    @Column(nullable = false, length = 100)
    private String recordedBy;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 100)
    private String reference;

    @Column(nullable = false)
    private LocalDateTime recordedAt = LocalDateTime.now();

    protected BillPayment() {
    }

    public BillPayment(Bill bill, String recordedBy, BigDecimal amount, String reference) {
        this.bill = bill;
        this.recordedBy = recordedBy;
        this.amount = amount;
        this.reference = reference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public String getReference() {
        return reference;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }
}
