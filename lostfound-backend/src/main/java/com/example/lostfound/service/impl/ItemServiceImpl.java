package com.example.lostfound.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.lostfound.cache.CacheKeys;
import com.example.lostfound.cache.TwoLevelCache;
import com.example.lostfound.domain.entity.LostItem;
import com.example.lostfound.domain.vo.ItemPageVO;
import com.example.lostfound.mapper.ItemMapper;
import com.example.lostfound.service.ItemService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl extends ServiceImpl<ItemMapper, LostItem> implements ItemService {

    private final TwoLevelCache cache;

    @Override
    public ItemPageVO page(String status, String itemType, String category, String keyword, int page, int size) {
        String key = CacheKeys.ITEM_PAGE_PREFIX + cacheKey(status, itemType, category, keyword, page, size);
        return cache.get(key, ItemPageVO.class,
                () -> loadPage(status, itemType, category, keyword, page, size));
    }

    @Override
    public LostItem findVisibleById(Long id) {
        return cache.get(CacheKeys.itemDetail(id), LostItem.class, () -> getById(id));
    }

    @Override
    public List<String> categories() {
        return cache.getList(CacheKeys.ITEM_CATEGORIES, String.class, () -> baseMapper.selectCategories());
    }

    @Override
    public List<LostItem> findByPublisherId(Long publisherId) {
        return list(Wrappers.<LostItem>lambdaQuery()
                .eq(LostItem::getPublisherId, publisherId)
                .ne(LostItem::getStatus, "DELETED")
                .orderByDesc(LostItem::getCreatedAt));
    }

    @Override
    public List<LostItem> findByStatus(String status) {
        return list(Wrappers.<LostItem>lambdaQuery()
                .eq(LostItem::getStatus, status)
                .orderByDesc(LostItem::getCreatedAt));
    }

    @Override
    public void updateStatus(Long id, String status) {
        LostItem item = new LostItem();
        item.setId(id);
        item.setStatus(status);
        updateById(item);
        evict(id);
    }

    @Override
    public int updateOpenStatus(Long id, String status) {
        int rows = baseMapper.update(null, Wrappers.<LostItem>lambdaUpdate()
                .eq(LostItem::getId, id)
                .eq(LostItem::getStatus, "OPEN")
                .set(LostItem::getStatus, status));
        if (rows > 0) {
            evict(id);
        }
        return rows;
    }

    @Override
    public long countAll() {
        return count(Wrappers.<LostItem>lambdaQuery().ne(LostItem::getStatus, "DELETED"));
    }

    @Override
    public long countByStatus(String status) {
        return count(Wrappers.<LostItem>lambdaQuery().eq(LostItem::getStatus, status));
    }

    @Override
    public List<Map<String, Object>> countByType() {
        return baseMapper.countByType();
    }

    @Override
    public List<Map<String, Object>> countByCategory() {
        return baseMapper.countByCategory();
    }

    @Override
    public void evict(Long id) {
        if (id != null) {
            cache.evict(CacheKeys.itemDetail(id));
        }
        cache.evictByPrefix(CacheKeys.ITEM_PAGE_PREFIX);
        cache.evict(CacheKeys.ITEM_CATEGORIES);
    }

    private ItemPageVO loadPage(String status, String itemType, String category, String keyword, int page, int size) {
        IPage<LostItem> result = baseMapper.selectItemPage(new Page<>(page, size), status, itemType, category, keyword);
        return new ItemPageVO(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    private String cacheKey(String status, String itemType, String category, String keyword, int page, int size) {
        String raw = String.join("|",
                text(status), text(itemType), text(category), text(keyword),
                String.valueOf(page), String.valueOf(size));
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
