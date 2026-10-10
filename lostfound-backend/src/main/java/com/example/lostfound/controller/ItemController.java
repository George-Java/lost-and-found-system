package com.example.lostfound.controller;

import java.util.List;

import com.example.lostfound.domain.entity.ClaimRecord;
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
import com.example.lostfound.common.ApiResponse;
import com.example.lostfound.common.BusinessException;
import com.example.lostfound.domain.dto.ItemCreateRequest;
import com.example.lostfound.domain.entity.LostItem;
import com.example.lostfound.service.ClaimService;
import com.example.lostfound.service.ItemService;
import com.example.lostfound.domain.vo.ItemPageVO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final ClaimService claimService;

    @GetMapping
    public ApiResponse<ItemPageVO> page(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        int validPage = Math.max(page, 1);
        int validSize = Math.max(1, Math.min(size, 50));
        String normalizedType = normalizeType(itemType);
        return ApiResponse.success(itemService.page(
                trimToNull(status), normalizedType, trimToNull(category), trimToNull(keyword), validPage, validSize));
    }

    @GetMapping("/categories")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.success(itemService.categories());
    }

    @GetMapping("/{id}")
    public ApiResponse<LostItem> detail(@PathVariable Long id) {
        LostItem item = itemService.findVisibleById(id);
        if (item == null || "DELETED".equals(item.getStatus())) {
            throw new BusinessException(404, "Item not found");
        }
        return ApiResponse.success(item);
    }

    @PostMapping
    public ApiResponse<LostItem> create(@RequestBody @Valid ItemCreateRequest request) {
        if ("ADMIN".equals(AuthContext.role())) {
            throw new BusinessException(403, "Admin cannot publish item posts");
        }
        LostItem item = new LostItem();
        item.setTitle(request.title());
        item.setDescription(request.description());
        item.setImageUrls(request.imageUrls());
        item.setCategory(request.category());
        item.setLocation(request.location());
        item.setLostTime(request.lostTime());
        item.setContact(request.contact());
        item.setItemType(normalizeType(request.itemType()));
        item.setPublisherId(AuthContext.userId());
        item.setStatus("PENDING");
        itemService.save(item);
        itemService.evict(item.getId());
        return ApiResponse.success(itemService.getById(item.getId()));
    }

    @GetMapping("/mine")
    public ApiResponse<List<LostItem>> mine() {
        return ApiResponse.success(itemService.findByPublisherId(AuthContext.userId()));
    }

    @GetMapping("/{id}/claims")
    public ApiResponse<List<ClaimRecord>> claims(@PathVariable Long id) {
        LostItem item = itemService.getById(id);
        if (item == null || "DELETED".equals(item.getStatus())) {
            throw new BusinessException(404, "Item not found");
        }
        boolean canView = item.getPublisherId().equals(AuthContext.userId()) || "ADMIN".equals(AuthContext.role());
        if (!canView) {
            throw new BusinessException(403, "No permission to view claims of this item");
        }
        return ApiResponse.success(claimService.findByItemId(id));
    }

    @PutMapping("/{id}/close")
    public ApiResponse<Void> close(@PathVariable Long id) {
        LostItem item = requireOperableItem(id, "close");
        if (!"OPEN".equals(item.getStatus())) {
            throw new BusinessException(400, "Only open items can be closed");
        }
        itemService.updateStatus(id, "CLOSED");
        return ApiResponse.success();
    }

    @PutMapping("/{id}/open")
    public ApiResponse<Void> open(@PathVariable Long id) {
        LostItem item = requireOperableItem(id, "open");
        if (!"CLOSED".equals(item.getStatus())) {
            throw new BusinessException(400, "Only closed items can be reopened");
        }
        itemService.updateStatus(id, "OPEN");
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        requireOperableItem(id, "delete");
        itemService.updateStatus(id, "DELETED");
        return ApiResponse.success();
    }

    private LostItem requireOperableItem(Long id, String action) {
        LostItem item = itemService.getById(id);
        if (item == null || "DELETED".equals(item.getStatus())) {
            throw new BusinessException(404, "Item not found");
        }
        boolean canOperate = item.getPublisherId().equals(AuthContext.userId()) || "ADMIN".equals(AuthContext.role());
        if (!canOperate) {
            throw new BusinessException(403, "No permission to " + action + " this item");
        }
        return item;
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
