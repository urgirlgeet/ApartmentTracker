package com.apartment.tracker.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ResidentProfileForm {
    @NotBlank(message = "Enter your full name.")
    @Size(max = 100, message = "Name must be at most 100 characters.")
    private String fullName;

    @NotBlank(message = "Enter a phone number.")
    @Pattern(regexp = "[+0-9() -]{7,30}", message = "Enter a valid phone number.")
    private String phone;

    private String apartmentBlock;

    private Integer apartmentUnit;

    @Email(message = "Enter a valid email address.")
    @Size(max = 150, message = "Email must be at most 150 characters.")
    private String email;

    @Size(max = 100, message = "Emergency contact name must be at most 100 characters.")
    private String emergencyContactName;

    @Pattern(regexp = "^$|[+0-9() -]{7,30}", message = "Enter a valid emergency contact number.")
    private String emergencyContactPhone;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getApartmentBlock() {
        return apartmentBlock;
    }

    public void setApartmentBlock(String apartmentBlock) {
        this.apartmentBlock = apartmentBlock == null ? null : apartmentBlock.toUpperCase();
    }

    public Integer getApartmentUnit() {
        return apartmentUnit;
    }

    public void setApartmentUnit(Integer apartmentUnit) {
        this.apartmentUnit = apartmentUnit;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    static ResidentProfileForm from(ResidentProfile profile) {
        ResidentProfileForm form = new ResidentProfileForm();
        form.setFullName(profile.getFullName());
        form.setPhone(profile.getPhone());
        String[] address = ApartmentAddress.parse(profile.getApartmentNumber());
        form.setApartmentBlock(address[0]);
        form.setApartmentUnit(address[1].isEmpty() ? null : Integer.valueOf(address[1]));
        form.setEmail(profile.getEmail());
        form.setEmergencyContactName(profile.getEmergencyContactName());
        form.setEmergencyContactPhone(profile.getEmergencyContactPhone());
        return form;
    }
}
