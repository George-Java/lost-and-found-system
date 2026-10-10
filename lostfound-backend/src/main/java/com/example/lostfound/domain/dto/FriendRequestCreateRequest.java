package com.example.lostfound.domain.dto;

import jakarta.validation.constraints.NotNull;

public record FriendRequestCreateRequest(
        @NotNull(message = "Target user is required")
        Long targetUserId) {
}
