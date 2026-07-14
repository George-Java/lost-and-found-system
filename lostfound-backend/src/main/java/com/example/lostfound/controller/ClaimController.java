package com.example.lostfound.controller;

import com.example.lostfound.auth.AuthContext;
import com.example.lostfound.auth.LoginRequired;
import com.example.lostfound.common.ApiResponse;
import com.example.lostfound.common.BusinessException;
import com.example.lostfound.dto.ClaimCreateRequest;
import com.example.lostfound.dto.ClaimReviewRequest;
import com.example.lostfound.entity.ClaimRecord;
import com.example.lostfound.entity.LostItem;
import com.example.lostfound.mapper.ClaimMapper;
import com.example.lostfound.mapper.ItemMapper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimMapper claimMapper;
    private final ItemMapper itemMapper;

    public ClaimController(ClaimMapper claimMapper, ItemMapper itemMapper) {
        this.claimMapper = claimMapper;
        this.itemMapper = itemMapper;
    }

    @LoginRequired
    @PostMapping
    public ApiResponse<ClaimRecord> create(@RequestBody @Valid ClaimCreateRequest request) {
        LostItem item = itemMapper.findById(request.getItemId());
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

        ClaimRecord pending = claimMapper.findPendingByItemAndClaimant(item.getId(), AuthContext.userId());
        if (pending != null) {
            throw new BusinessException(409, "You already submitted a pending claim");
        }

        ClaimRecord record = new ClaimRecord();
        record.setItemId(request.getItemId());
        record.setClaimantId(AuthContext.userId());
        record.setClaimReason(request.getClaimReason().trim());
        record.setProofDescription(trimToNull(request.getProofDescription()));
        record.setProofMaterials(trimToNull(request.getProofMaterials()));
        record.setStatus("PENDING");
        claimMapper.insert(record);
        return ApiResponse.success(claimMapper.findById(record.getId()));
    }

    @LoginRequired
    @GetMapping("/mine")
    public ApiResponse<List<ClaimRecord>> mine() {
        return ApiResponse.success(claimMapper.findByClaimantId(AuthContext.userId()));
    }

    @LoginRequired(adminOnly = true)
    @GetMapping
    public ApiResponse<List<ClaimRecord>> list(@RequestParam(required = false) String status) {
        return ApiResponse.success(claimMapper.findList(normalizeStatus(status, true)));
    }

    @LoginRequired(adminOnly = true)
    @GetMapping("/pending")
    public ApiResponse<List<ClaimRecord>> pending() {
        return ApiResponse.success(claimMapper.findList("PENDING"));
    }

    @LoginRequired(adminOnly = true)
    @PutMapping("/{id}/review")
    public ApiResponse<Void> review(@PathVariable Long id, @RequestBody @Valid ClaimReviewRequest request) {
        ClaimRecord claim = claimMapper.findById(id);
        if (claim == null) {
            throw new BusinessException(404, "Claim not found");
        }
        if (!"PENDING".equals(claim.getStatus())) {
            throw new BusinessException(400, "Claim has already been reviewed");
        }

        LostItem item = itemMapper.findById(claim.getItemId());
        if (item == null) {
            throw new BusinessException(404, "Item not found");
        }
        String status = normalizeStatus(request.getStatus(), false);
        if ("PENDING".equals(status)) {
            throw new BusinessException(400, "Review status must be APPROVED or REJECTED");
        }

        LocalDateTime now = LocalDateTime.now();
        String reviewNote = trimToNull(request.getReviewNote());
        claimMapper.review(id, status, AuthContext.userId(), reviewNote, now);

        if ("APPROVED".equals(status)) {
            itemMapper.updateStatus(claim.getItemId(), "DELETED");
            claimMapper.rejectOthersAfterApproved(claim.getItemId(), id, AuthContext.userId(), "Rejected because another claim was approved", now);
        }
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
