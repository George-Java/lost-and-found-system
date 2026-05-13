package com.example.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ClaimReviewRequest {
    @NotBlank(message = "Review status is required")
    private String status;

    @Size(max = 500, message = "Review note length must be <= 500")
    private String reviewNote;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }
}
