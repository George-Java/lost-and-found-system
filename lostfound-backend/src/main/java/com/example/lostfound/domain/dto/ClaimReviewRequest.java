package com.example.lostfound.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClaimReviewRequest(
        @NotBlank(message = "Review status is required")
        String status,

        @Size(max = 500, message = "Review note length must be <= 500")
        String reviewNote) {
}
