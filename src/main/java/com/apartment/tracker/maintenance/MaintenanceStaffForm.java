package com.apartment.tracker.maintenance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class MaintenanceStaffForm {
    @NotBlank(message = "Enter the staff member's name.")
    @Size(max = 100, message = "Name must be at most 100 characters.")
    private String name;

    @NotBlank(message = "Choose or enter a specialization.")
    @Size(max = 60, message = "Specialization must be at most 60 characters.")
    private String specialization;

    @Pattern(regexp = "^$|[+0-9() -]{7,30}", message = "Enter a valid phone number.")
    private String phone;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
