package com.example.lostfound.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record UserRoleUpdateRequest(
        @NotBlank(message = "Role is required")
        String role) {
}
