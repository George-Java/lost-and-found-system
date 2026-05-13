package com.example.lostfound.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
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
        int offset = (validPage - 1) * validSize;

        List<LostItem> records = itemMapper.findPage(status, normalizeType(itemType), category, keyword, offset, validSize);
        long total = itemMapper.count(status, normalizeType(itemType), category, keyword);

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", validPage);
        result.put("size", validSize);
        result.put("records", records);
        return ApiResponse.success(result);
    }

    @GetMapping("/categories")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.success(itemMapper.findCategories());
    }

    @GetMapping("/{id}")
    public ApiResponse<LostItem> detail(@PathVariable Long id) {
        LostItem item = itemMapper.findById(id);
        if (item == null) {
            throw new BusinessException(404, "Item not found");
        }
        return ApiResponse.success(item);
    }

    @LoginRequired
    @PostMapping
    public ApiResponse<LostItem> create(@RequestBody @Valid ItemCreateRequest request) {
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
        item.setStatus("OPEN");
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
        if (item == null) {
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
        if (item == null) {
            throw new BusinessException(404, "Item not found");
        }
        boolean canOperate = item.getPublisherId().equals(AuthContext.userId()) || "ADMIN".equals(AuthContext.role());
        if (!canOperate) {
            throw new BusinessException(403, "No permission to close this item");
        }
        itemMapper.updateStatus(id, "CLOSED");
        return ApiResponse.success();
    }

    @LoginRequired
    @PutMapping("/{id}/open")
    public ApiResponse<Void> open(@PathVariable Long id) {
        LostItem item = itemMapper.findById(id);
        if (item == null) {
            throw new BusinessException(404, "Item not found");
        }
        boolean canOperate = item.getPublisherId().equals(AuthContext.userId()) || "ADMIN".equals(AuthContext.role());
        if (!canOperate) {
            throw new BusinessException(403, "No permission to open this item");
        }
        itemMapper.updateStatus(id, "OPEN");
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
}
