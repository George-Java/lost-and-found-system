package com.example.lostfound.domain.vo;

import com.example.lostfound.domain.entity.LostItem;

import java.util.List;

/** 物品列表分页结果，可整体放进缓存。 */
public record ItemPageVO(long total, long page, long size, List<LostItem> records) {
}
