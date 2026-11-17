package com.apartment.tracker.complaint;

import com.apartment.tracker.maintenance.MaintenanceDepartment;

public enum ComplaintCategory {
    PLUMBING("Plumbing"),
    ELECTRICAL("Electrical"),
    CLEANING("Cleaning"),
    LIFT("Lift"),
    INTERNET("Internet"),
    WATER("Water"),
    SECURITY("Security"),
    OTHER("Other");

    private final String displayName;

    ComplaintCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCodePrefix() {
        return switch (this) {
            case PLUMBING -> "P";
            case ELECTRICAL -> "E";
            case CLEANING -> "C";
            case LIFT -> "L";
            case INTERNET -> "I";
            case WATER -> "W";
            case SECURITY -> "S";
            case OTHER -> "O";
        };
    }

    public MaintenanceDepartment getDepartment() {
        return switch (this) {
            case PLUMBING -> MaintenanceDepartment.PLUMBING;
            case WATER -> MaintenanceDepartment.WATER;
            case ELECTRICAL -> MaintenanceDepartment.ELECTRICAL;
            case CLEANING -> MaintenanceDepartment.CLEANING;
            case LIFT -> MaintenanceDepartment.LIFT;
            case SECURITY -> MaintenanceDepartment.SECURITY;
            case INTERNET, OTHER -> MaintenanceDepartment.GENERAL;
        };
    }
}
