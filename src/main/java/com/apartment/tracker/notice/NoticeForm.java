package com.apartment.tracker.notice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class NoticeForm {
    @NotBlank(message = "Enter a notice title.")
    @Size(max = 100, message = "Title must be at most 100 characters.")
    private String title;

    @NotBlank(message = "Enter the notice details.")
    @Size(max = 2000, message = "Description must be at most 2000 characters.")
    private String description;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
