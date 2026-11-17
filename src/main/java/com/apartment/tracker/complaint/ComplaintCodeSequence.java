package com.apartment.tracker.complaint;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class ComplaintCodeSequence {
    @Id
    @Column(length = 30)
    private String category;

    @Column(nullable = false)
    private int nextNumber;

    protected ComplaintCodeSequence() {
    }

    public ComplaintCodeSequence(String category, int nextNumber) {
        this.category = category;
        this.nextNumber = nextNumber;
    }

    public int getNextNumber() {
        return nextNumber;
    }

    public void setNextNumber(int nextNumber) {
        this.nextNumber = nextNumber;
    }
}
