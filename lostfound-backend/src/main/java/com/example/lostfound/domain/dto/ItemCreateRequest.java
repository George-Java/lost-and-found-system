package com.example.lostfound.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ItemCreateRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title length must be <= 100")
        String title,

        @Size(max = 1000, message = "Description length must be <= 1000")
        String description,

        @Size(max = 4000, message = "Image URL length must be <= 4000")
        String imageUrls,

        @Size(max = 50, message = "Category length must be <= 50")
        String category,

        @Size(max = 120, message = "Location length must be <= 120")
        String location,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime lostTime,

        @Size(max = 120, message = "Contact length must be <= 120")
        String contact,

        @NotBlank(message = "Item type is required")
        String itemType) {
}
