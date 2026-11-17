package com.apartment.tracker.maintenance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

@Entity
public class MaintenanceStaffCodeSequence {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private MaintenanceDepartment department;

    @Column(nullable = false)
    private int nextNumber;

    protected MaintenanceStaffCodeSequence() {
    }

    public MaintenanceStaffCodeSequence(MaintenanceDepartment department, int nextNumber) {
        this.department = department;
        this.nextNumber = nextNumber;
    }

    public int getNextNumber() {
        return nextNumber;
    }

    public void setNextNumber(int nextNumber) {
        this.nextNumber = nextNumber;
    }
}
