package com.example.lostfound.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.lostfound.auth.AuthContext;
import com.example.lostfound.auth.LoginRequired;
import com.example.lostfound.common.ApiResponse;
import com.example.lostfound.common.BusinessException;
import com.example.lostfound.dto.ItemCreateRequest;
import com.example.lostfound.entity.ClaimRecord;
import com.example.lostfound.entity.LostItem;
import com.example.lostfound.mapper.ClaimMapper;
import com.example.lostfound.mapper.ItemMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemMapper itemMapper;
    private final ClaimMapper claimMapper;

    public ItemController(ItemMapper itemMapper, ClaimMapper claimMapper) {
        this.itemMapper = itemMapper;
        this.claimMapper = claimMapper;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> page(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        int validPage = Math.max(page, 1);
        int validSize = Math.max(1, Math.min(size, 50));
        String normalizedType = normalizeType(itemType);

        LambdaQueryWrapper<LostItem> query = Wrappers.<LostItem>lambdaQuery()
                .eq(hasText(status), LostItem::getStatus, trimToNull(status))
                .eq(!hasText(status), LostItem::getStatus, "OPEN")
                .ne(hasText(status), LostItem::getStatus, "DELETED")
                .eq(hasText(normalizedType), LostItem::getItemType, normalizedType)
                .eq(hasText(category), LostItem::getCategory, trimToNull(category))
                .and(hasText(keyword), wrapper -> {
                    String value = trimToNull(keyword);
                    wrapper.like(LostItem::getTitle, value)
                            .or()
                            .like(LostItem::getDescription, value)
                            .or()
                            .like(LostItem::getLocation, value);
                })
                .orderByDesc(LostItem::getCreatedAt);
        Page<LostItem> pageResult = itemMapper.selectPage(new Page<>(validPage, validSize), query);

        Map<String, Object> result = new HashMap<>();
        result.put("total", pageResult.getTotal());
        result.put("page", validPage);
        result.put("size", validSize);
        result.put("records", pageResult.getRecords());
        return ApiResponse.success(result);
    }

    @GetMapping("/categories")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.success(itemMapper.findCategories());
    }

    @GetMapping("/{id}")
    public ApiResponse<LostItem> detail(@PathVariable Long id) {
        LostItem item = itemMapper.findById(id);
        if (item == null || "DELETED".equals(item.getStatus())) {
            throw new BusinessException(404, "Item not found");
        }
        return ApiResponse.success(item);
    }

    @LoginRequired
    @PostMapping
    public ApiResponse<LostItem> create(@RequestBody @Valid ItemCreateRequest request) {
        if ("ADMIN".equals(AuthContext.role())) {
            throw new BusinessException(403, "Admin cannot publish item posts");
        }
        LostItem item = new LostItem();
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setImageUrls(request.getImageUrls());
        item.setCategory(request.getCategory());
        item.setLocation(request.getLocation());
        item.setLostTime(request.getLostTime());
        item.setContact(request.getContact());
        item.setItemType(normalizeType(request.getItemType()));
        item.setPublisherId(AuthContext.userId());
        item.setStatus("PENDING");
        itemMapper.insert(item);
        return ApiResponse.success(itemMapper.findById(item.getId()));
    }

    @LoginRequired
    @GetMapping("/mine")
    public ApiResponse<List<LostItem>> mine() {
        return ApiResponse.success(itemMapper.findByPublisherId(AuthContext.userId()));
    }

    @LoginRequired
    @GetMapping("/{id}/claims")
    public ApiResponse<List<ClaimRecord>> claims(@PathVariable Long id) {
        LostItem item = itemMapper.findById(id);
        if (item == null || "DELETED".equals(item.getStatus())) {
            throw new BusinessException(404, "Item not found");
        }
        boolean canView = item.getPublisherId().equals(AuthContext.userId()) || "ADMIN".equals(AuthContext.role());
        if (!canView) {
            throw new BusinessException(403, "No permission to view claims of this item");
        }
        return ApiResponse.success(claimMapper.findByItemId(id));
    }

    @LoginRequired
    @PutMapping("/{id}/close")
    public ApiResponse<Void> close(@PathVariable Long id) {
        LostItem item = itemMapper.findById(id);
        if (item == null || "DELETED".equals(item.getStatus())) {
            throw new BusinessException(404, "Item not found");
        }
        boolean canOperate = item.getPublisherId().equals(AuthContext.userId()) || "ADMIN".equals(AuthContext.role());
        if (!canOperate) {
            throw new BusinessException(403, "No permission to close this item");
        }
        if (!"OPEN".equals(item.getStatus())) {
            throw new BusinessException(400, "Only open items can be closed");
        }
        itemMapper.updateStatus(id, "CLOSED");
        return ApiResponse.success();
    }

    @LoginRequired
    @PutMapping("/{id}/open")
    public ApiResponse<Void> open(@PathVariable Long id) {
        LostItem item = itemMapper.findById(id);
        if (item == null || "DELETED".equals(item.getStatus())) {
            throw new BusinessException(404, "Item not found");
        }
        boolean canOperate = item.getPublisherId().equals(AuthContext.userId()) || "ADMIN".equals(AuthContext.role());
        if (!canOperate) {
            throw new BusinessException(403, "No permission to open this item");
        }
        if (!"CLOSED".equals(item.getStatus())) {
            throw new BusinessException(400, "Only closed items can be reopened");
        }
        itemMapper.updateStatus(id, "OPEN");
        return ApiResponse.success();
    }

    @LoginRequired
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        LostItem item = itemMapper.findById(id);
        if (item == null || "DELETED".equals(item.getStatus())) {
            throw new BusinessException(404, "Item not found");
        }
        boolean canOperate = item.getPublisherId().equals(AuthContext.userId()) || "ADMIN".equals(AuthContext.role());
        if (!canOperate) {
            throw new BusinessException(403, "No permission to delete this item");
        }
        itemMapper.updateStatus(id, "DELETED");
        return ApiResponse.success();
    }

    private String normalizeType(String itemType) {
        if (itemType == null || itemType.isBlank()) {
            return null;
        }
        String normalized = itemType.trim().toUpperCase();
        if (!"LOST".equals(normalized) && !"FOUND".equals(normalized)) {
            throw new BusinessException(400, "Item type must be LOST or FOUND");
        }
        return normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
