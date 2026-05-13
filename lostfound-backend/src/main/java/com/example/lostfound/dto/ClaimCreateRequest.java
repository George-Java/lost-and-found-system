package com.example.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ClaimCreateRequest {
    @NotNull(message = "Item id is required")
    private Long itemId;

    @NotBlank(message = "Claim reason is required")
    @Size(max = 1000, message = "Claim reason length must be <= 1000")
    private String claimReason;

    @Size(max = 1000, message = "Proof description length must be <= 1000")
    private String proofDescription;

    @Size(max = 4000, message = "Proof material URL length must be <= 4000")
    private String proofMaterials;

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getClaimReason() {
        return claimReason;
    }

    public void setClaimReason(String claimReason) {
        this.claimReason = claimReason;
    }

    public String getProofDescription() {
        return proofDescription;
    }

    public void setProofDescription(String proofDescription) {
        this.proofDescription = proofDescription;
    }

    public String getProofMaterials() {
        return proofMaterials;
    }

    public void setProofMaterials(String proofMaterials) {
        this.proofMaterials = proofMaterials;
    }
}
