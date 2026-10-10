package com.example.lostfound.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.lostfound.domain.entity.LostItem;
import com.example.lostfound.domain.vo.ItemPageVO;

import java.util.List;
import java.util.Map;

public interface ItemService extends IService<LostItem> {

    /** 列表查询命中两级缓存，未命中才回源数据库。 */
    ItemPageVO page(String status, String itemType, String category, String keyword, int page, int size);

    LostItem findVisibleById(Long id);

    List<String> categories();

    List<LostItem> findByPublisherId(Long publisherId);

    List<LostItem> findByStatus(String status);

    void updateStatus(Long id, String status);

    /** 认领通过时的状态流转：只有 OPEN 才能改成目标状态，返回影响行数供并发校验。 */
    int updateOpenStatus(Long id, String status);

    long countAll();

    long countByStatus(String status);

    List<Map<String, Object>> countByType();

    List<Map<String, Object>> countByCategory();

    /** 物品发生变化时清掉它的详情、列表和分类缓存。 */
    void evict(Long id);
}
