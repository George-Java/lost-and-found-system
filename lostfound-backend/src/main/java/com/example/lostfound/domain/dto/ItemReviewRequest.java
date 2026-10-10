package com.example.lostfound.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record ItemReviewRequest(
        @NotBlank(message = "Review status is required")
        String status) {
}
