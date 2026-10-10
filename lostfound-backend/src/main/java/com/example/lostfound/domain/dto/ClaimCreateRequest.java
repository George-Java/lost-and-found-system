package com.example.lostfound.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClaimCreateRequest(
        @NotNull(message = "Item id is required")
        Long itemId,

        @NotBlank(message = "Claim reason is required")
        @Size(max = 1000, message = "Claim reason length must be <= 1000")
        String claimReason,

        @Size(max = 1000, message = "Proof description length must be <= 1000")
        String proofDescription,

        @Size(max = 4000, message = "Proof material URL length must be <= 4000")
        String proofMaterials) {
}
