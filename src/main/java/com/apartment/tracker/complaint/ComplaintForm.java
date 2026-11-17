package com.apartment.tracker.complaint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class ComplaintForm {
    @NotNull(message = "Choose a complaint category.")
    private ComplaintCategory category;

    @NotBlank(message = "Describe the issue.")
    @Size(max = 1000, message = "Description must be at most 1000 characters.")
    private String description;

    private MultipartFile image;

    public ComplaintCategory getCategory() {
        return category;
    }

    public void setCategory(ComplaintCategory category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MultipartFile getImage() {
        return image;
    }

    public void setImage(MultipartFile image) {
        this.image = image;
    }
}
