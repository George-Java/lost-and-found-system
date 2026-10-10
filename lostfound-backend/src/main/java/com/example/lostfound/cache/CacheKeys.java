package com.example.lostfound.cache;

/** 缓存键统一在这里定义，业务与 Canal 同步共用，避免两边写错前缀。 */
public final class CacheKeys {

    public static final String ITEM_PREFIX = "lostfound:item:";
    public static final String ITEM_DETAIL_PREFIX = ITEM_PREFIX + "detail:";
    public static final String ITEM_PAGE_PREFIX = ITEM_PREFIX + "page:";
    public static final String ITEM_CATEGORIES = ITEM_PREFIX + "categories";

    private CacheKeys() {
    }

    public static String itemDetail(Long id) {
        return ITEM_DETAIL_PREFIX + id;
    }
}
