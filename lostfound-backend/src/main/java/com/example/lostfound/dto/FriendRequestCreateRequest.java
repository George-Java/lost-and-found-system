package com.example.lostfound.dto;

import jakarta.validation.constraints.NotNull;

public class FriendRequestCreateRequest {
    @NotNull(message = "Target user is required")
    private Long targetUserId;

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }
}
