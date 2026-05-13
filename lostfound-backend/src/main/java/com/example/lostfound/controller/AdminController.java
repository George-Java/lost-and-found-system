package com.example.lostfound.controller;

import com.example.lostfound.auth.AuthContext;
import com.example.lostfound.auth.LoginRequired;
import com.example.lostfound.common.ApiResponse;
import com.example.lostfound.common.BusinessException;
import com.example.lostfound.dto.UserRoleUpdateRequest;
import com.example.lostfound.entity.UserAccount;
import com.example.lostfound.mapper.ClaimMapper;
import com.example.lostfound.mapper.ItemMapper;
import com.example.lostfound.mapper.UserMapper;
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

@RestController
@RequestMapping("/api/admin")
@LoginRequired(adminOnly = true)
public class AdminController {

    private final ItemMapper itemMapper;
    private final ClaimMapper claimMapper;
    private final UserMapper userMapper;

    public AdminController(ItemMapper itemMapper, ClaimMapper claimMapper, UserMapper userMapper) {
        this.itemMapper = itemMapper;
        this.claimMapper = claimMapper;
        this.userMapper = userMapper;
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userMapper.countAll());
        stats.put("itemCount", itemMapper.countAll());
        stats.put("openItemCount", itemMapper.countByStatus("OPEN"));
        stats.put("matchedItemCount", itemMapper.countByStatus("MATCHED"));
        stats.put("closedItemCount", itemMapper.countByStatus("CLOSED"));
        stats.put("claimCount", claimMapper.countAll());
        stats.put("pendingClaimCount", claimMapper.countByStatus("PENDING"));
        stats.put("approvedClaimCount", claimMapper.countByStatus("APPROVED"));
        stats.put("rejectedClaimCount", claimMapper.countByStatus("REJECTED"));
        stats.put("typeStats", itemMapper.countByType());
        stats.put("categoryStats", itemMapper.countByCategory());
        return ApiResponse.success(stats);
    }

    @GetMapping("/users")
    public ApiResponse<List<UserAccount>> users() {
        return ApiResponse.success(userMapper.findAll());
    }

    @PutMapping("/users/{id}/role")
    public ApiResponse<Void> updateRole(@PathVariable Long id, @RequestBody @Valid UserRoleUpdateRequest request) {
        UserAccount target = userMapper.findById(id);
        if (target == null) {
            throw new BusinessException(404, "User not found");
        }
        if (AuthContext.userId().equals(id)) {
            throw new BusinessException(400, "Admin cannot change own role");
        }

        String role = normalizeRole(request.getRole());
        userMapper.updateRole(id, role);
        return ApiResponse.success();
    }

    private String normalizeRole(String role) {
        String normalized = role.trim().toUpperCase();
        if (!"USER".equals(normalized) && !"ADMIN".equals(normalized)) {
            throw new BusinessException(400, "Role must be USER or ADMIN");
        }
        return normalized;
    }
}
