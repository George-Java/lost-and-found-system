package com.example.lostfound.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.lostfound.entity.LostItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ItemMapper extends BaseMapper<LostItem> {

    default LostItem findById(Long id) {
        return selectById(id);
    }

    default List<LostItem> findByPublisherId(Long publisherId) {
        return selectList(Wrappers.<LostItem>lambdaQuery()
                .eq(LostItem::getPublisherId, publisherId)
                .ne(LostItem::getStatus, "DELETED")
                .orderByDesc(LostItem::getCreatedAt));
    }

    default List<LostItem> findByStatus(String status) {
        return selectList(Wrappers.<LostItem>lambdaQuery()
                .eq(LostItem::getStatus, status)
                .orderByDesc(LostItem::getCreatedAt));
    }

    default List<String> findCategories() {
        return selectObjs(new QueryWrapper<LostItem>()
                .select("distinct category")
                .isNotNull("category")
                .ne("category", "")
                .ne("status", "DELETED")
                .orderByAsc("category"))
                .stream()
                .map(String::valueOf)
                .toList();
    }

    default int updateStatus(Long id, String status) {
        LostItem item = new LostItem();
        item.setId(id);
        item.setStatus(status);
        return updateById(item);
    }

    default long countAll() {
        return selectCount(Wrappers.<LostItem>lambdaQuery().ne(LostItem::getStatus, "DELETED"));
    }

    default long countByStatus(String status) {
        return selectCount(Wrappers.<LostItem>lambdaQuery().eq(LostItem::getStatus, status));
    }

    @Select("select item_type as name, count(1) as value from lost_item where status != 'DELETED' group by item_type")
    List<Map<String, Object>> countByType();

    @Select("select category as name, count(1) as value from lost_item where status != 'DELETED' and category is not null and category != '' group by category order by value desc limit 8")
    List<Map<String, Object>> countByCategory();
}
