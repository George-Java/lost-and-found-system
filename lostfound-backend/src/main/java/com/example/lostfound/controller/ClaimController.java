package com.example.lostfound.controller;

import com.example.lostfound.auth.AuthContext;
import com.example.lostfound.common.ApiResponse;
import com.example.lostfound.common.BusinessException;
import com.example.lostfound.domain.dto.ClaimCreateRequest;
import com.example.lostfound.domain.dto.ClaimReviewRequest;
import com.example.lostfound.domain.entity.ClaimRecord;
import com.example.lostfound.domain.entity.LostItem;
import com.example.lostfound.service.ClaimReviewService;
import com.example.lostfound.service.ClaimService;
import com.example.lostfound.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;
    private final ItemService itemService;
    private final ClaimReviewService claimReviewService;

    @PostMapping
    public ApiResponse<ClaimRecord> create(@RequestBody @Valid ClaimCreateRequest request) {
        LostItem item = itemService.getById(request.itemId());
        if (item == null) {
            throw new BusinessException(404, "Item not found");
        }
        if (!"OPEN".equals(item.getStatus())) {
            throw new BusinessException(400, "Item is not open for claims");
        }
        if ("ADMIN".equals(AuthContext.role())) {
            throw new BusinessException(403, "Admin cannot submit claim");
        }
        if (item.getPublisherId().equals(AuthContext.userId())) {
            throw new BusinessException(400, "Publisher cannot claim own item");
        }

        ClaimRecord pending = claimService.findPendingByItemAndClaimant(item.getId(), AuthContext.userId());
        if (pending != null) {
            throw new BusinessException(409, "You already submitted a pending claim");
        }

        ClaimRecord record = new ClaimRecord();
        record.setItemId(request.itemId());
        record.setClaimantId(AuthContext.userId());
        record.setClaimReason(request.claimReason().trim());
        record.setProofDescription(trimToNull(request.proofDescription()));
        record.setProofMaterials(trimToNull(request.proofMaterials()));
        record.setStatus("PENDING");
        claimService.save(record);
        return ApiResponse.success(claimService.findDetail(record.getId()));
    }

    @GetMapping("/mine")
    public ApiResponse<List<ClaimRecord>> mine() {
        return ApiResponse.success(claimService.findByClaimantId(AuthContext.userId()));
    }

    @GetMapping
    public ApiResponse<List<ClaimRecord>> list(@RequestParam(required = false) String status) {
        return ApiResponse.success(claimService.findList(normalizeStatus(status, true)));
    }

    @GetMapping("/pending")
    public ApiResponse<List<ClaimRecord>> pending() {
        return ApiResponse.success(claimService.findList("PENDING"));
    }

    @PutMapping("/{id}/review")
    public ApiResponse<Void> review(@PathVariable Long id, @RequestBody @Valid ClaimReviewRequest request) {
        String status = normalizeStatus(request.status(), false);
        if ("PENDING".equals(status)) {
            throw new BusinessException(400, "Review status must be APPROVED or REJECTED");
        }
        claimReviewService.review(id, status, AuthContext.userId(), trimToNull(request.reviewNote()));
        return ApiResponse.success();
    }

    private String normalizeStatus(String status, boolean allowBlank) {
        if (status == null || status.isBlank()) {
            if (allowBlank) {
                return null;
            }
            throw new BusinessException(400, "Status is required");
        }
        String normalized = status.trim().toUpperCase();
        if (!"PENDING".equals(normalized) && !"APPROVED".equals(normalized) && !"REJECTED".equals(normalized)) {
            throw new BusinessException(400, "Status must be PENDING, APPROVED or REJECTED");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
