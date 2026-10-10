package com.example.lostfound.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 4, max = 20, message = "Username length must be 4-20")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 30, message = "Password length must be 6-30")
        String password,

        @NotBlank(message = "Real name is required")
        String realName,

        String phone) {
}
