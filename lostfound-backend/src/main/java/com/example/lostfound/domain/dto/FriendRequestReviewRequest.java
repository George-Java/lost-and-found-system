package com.example.lostfound.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record FriendRequestReviewRequest(
        @NotBlank(message = "Review status is required")
        String status) {
}
