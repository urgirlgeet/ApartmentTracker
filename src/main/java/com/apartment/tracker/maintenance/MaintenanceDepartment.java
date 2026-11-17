package com.apartment.tracker.maintenance;

import com.apartment.tracker.complaint.ComplaintCategory;
import java.util.List;

public enum MaintenanceDepartment {
    ELECTRICAL("Electrical", List.of(ComplaintCategory.ELECTRICAL)),
    PLUMBING("Plumbing", List.of(ComplaintCategory.PLUMBING)),
    WATER("Water", List.of(ComplaintCategory.WATER)),
    CLEANING("Cleaning", List.of(ComplaintCategory.CLEANING)),
    LIFT("Lift", List.of(ComplaintCategory.LIFT)),
    SECURITY("Security", List.of(ComplaintCategory.SECURITY)),
    GENERAL("General maintenance", List.of(ComplaintCategory.INTERNET, ComplaintCategory.OTHER));

    private final String displayName;
    private final List<ComplaintCategory> complaintCategories;

    MaintenanceDepartment(String displayName, List<ComplaintCategory> complaintCategories) {
        this.displayName = displayName;
        this.complaintCategories = complaintCategories;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getStaffCodePrefix() {
        return switch (this) {
            case LIFT -> "SL";
            case PLUMBING -> "SP";
            case ELECTRICAL -> "SE";
            case WATER -> "SW";
            case CLEANING -> "SC";
            case SECURITY -> "SS";
            case GENERAL -> "SG";
        };
    }

    public List<ComplaintCategory> getComplaintCategories() {
        return complaintCategories;
    }
}
