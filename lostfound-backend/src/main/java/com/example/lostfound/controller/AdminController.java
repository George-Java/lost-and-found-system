package com.example.lostfound.controller;

import com.example.lostfound.auth.AuthContext;
import com.example.lostfound.common.ApiResponse;
import com.example.lostfound.common.BusinessException;
import com.example.lostfound.domain.dto.ItemReviewRequest;
import com.example.lostfound.domain.dto.UserRoleUpdateRequest;
import com.example.lostfound.domain.entity.LostItem;
import com.example.lostfound.domain.entity.UserAccount;
import com.example.lostfound.service.ClaimService;
import com.example.lostfound.service.ItemService;
import com.example.lostfound.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ItemService itemService;
    private final ClaimService claimService;
    private final UserService userService;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userService.countAll());
        stats.put("itemCount", itemService.countAll());
        stats.put("pendingItemCount", itemService.countByStatus("PENDING"));
        stats.put("openItemCount", itemService.countByStatus("OPEN"));
        stats.put("matchedItemCount", itemService.countByStatus("MATCHED"));
        stats.put("closedItemCount", itemService.countByStatus("CLOSED"));
        stats.put("claimCount", claimService.countAll());
        stats.put("pendingClaimCount", claimService.countByStatus("PENDING"));
        stats.put("approvedClaimCount", claimService.countByStatus("APPROVED"));
        stats.put("rejectedClaimCount", claimService.countByStatus("REJECTED"));
        stats.put("typeStats", itemService.countByType());
        stats.put("categoryStats", itemService.countByCategory());
        return ApiResponse.success(stats);
    }

    @GetMapping("/users")
    public ApiResponse<List<UserAccount>> users() {
        return ApiResponse.success(userService.findAll());
    }

    @GetMapping("/items/pending")
    public ApiResponse<List<LostItem>> pendingItems() {
        return ApiResponse.success(itemService.findByStatus("PENDING"));
    }

    @PutMapping("/items/{id}/review")
    public ApiResponse<Void> reviewItem(@PathVariable Long id, @RequestBody @Valid ItemReviewRequest request) {
        LostItem item = itemService.getById(id);
        if (item == null) {
            throw new BusinessException(404, "Item not found");
        }
        if (!"PENDING".equals(item.getStatus())) {
            throw new BusinessException(400, "Only pending item posts can be reviewed");
        }

        String status = normalizeItemReviewStatus(request.status());
        itemService.updateStatus(id, status);
        return ApiResponse.success();
    }

    @PutMapping("/users/{id}/role")
    public ApiResponse<Void> updateRole(@PathVariable Long id, @RequestBody @Valid UserRoleUpdateRequest request) {
        UserAccount target = userService.findById(id);
        if (target == null) {
            throw new BusinessException(404, "User not found");
        }
        if (AuthContext.userId().equals(id)) {
            throw new BusinessException(400, "Admin cannot change own role");
        }

        String role = normalizeRole(request.role());
        userService.updateRole(id, role);
        return ApiResponse.success();
    }

    private String normalizeRole(String role) {
        String normalized = role.trim().toUpperCase();
        if (!"USER".equals(normalized) && !"ADMIN".equals(normalized)) {
            throw new BusinessException(400, "Role must be USER or ADMIN");
        }
        return normalized;
    }

    private String normalizeItemReviewStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!"OPEN".equals(normalized) && !"REJECTED".equals(normalized)) {
            throw new BusinessException(400, "Status must be OPEN or REJECTED");
        }
        return normalized;
    }
}
