package com.apartment.tracker.account;

import com.apartment.tracker.profile.ResidentProfileForm;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;

public class ResidentSignupForm {
    @NotBlank(message = "Choose a username.")
    @Size(min = 3, max = 50, message = "Username must be 3 to 50 characters.")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Use letters, numbers, dots, underscores, or hyphens.")
    private String username;

    @NotBlank(message = "Choose a password.")
    @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters.")
    private String password;

    @NotBlank(message = "Confirm your password.")
    private String confirmPassword;

    @Valid
    private ResidentProfileForm profile = new ResidentProfileForm();

    @AssertTrue(message = "Passwords do not match.")
    public boolean isPasswordConfirmed() {
        return password == null || confirmPassword == null || password.equals(confirmPassword);
    }

    @AssertTrue(message = "Password must be 72 bytes or less.")
    public boolean isPasswordWithinEncoderLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public ResidentProfileForm getProfile() {
        return profile;
    }

    public void setProfile(ResidentProfileForm profile) {
        this.profile = profile;
    }
}
