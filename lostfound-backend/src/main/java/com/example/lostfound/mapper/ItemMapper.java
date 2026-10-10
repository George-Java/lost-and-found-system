package com.example.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.lostfound.domain.entity.LostItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 物品表 Mapper：简单增删改查由 ServiceImpl 提供，复杂查询写在 resources/mapper/ItemMapper.xml。
 */
@Mapper
public interface ItemMapper extends BaseMapper<LostItem> {

    /** 带动态条件的列表分页查询。 */
    IPage<LostItem> selectItemPage(IPage<LostItem> page,
                                   @Param("status") String status,
                                   @Param("itemType") String itemType,
                                   @Param("category") String category,
                                   @Param("keyword") String keyword);

    /** 所有已发布帖子的分类。 */
    List<String> selectCategories();

    /** 后台统计：按物品类型分组计数。 */
    List<Map<String, Object>> countByType();

    /** 后台统计：按分类分组计数，取前 8。 */
    List<Map<String, Object>> countByCategory();
}
