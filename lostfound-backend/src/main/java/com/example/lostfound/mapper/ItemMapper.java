package com.example.lostfound.mapper;

import com.example.lostfound.entity.LostItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ItemMapper {

    @Insert("""
            insert into lost_item(title, description, image_urls, category, location, lost_time, contact, item_type, publisher_id, status)
            values(#{title}, #{description}, #{imageUrls}, #{category}, #{location}, #{lostTime}, #{contact}, #{itemType}, #{publisherId}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LostItem item);

    @Select("select * from lost_item where id = #{id} limit 1")
    LostItem findById(@Param("id") Long id);

    @Select("""
            <script>
            select * from lost_item
            where 1=1
            <if test="status != null and status != ''">
                and status = #{status}
            </if>
            <if test="itemType != null and itemType != ''">
                and item_type = #{itemType}
            </if>
            <if test="category != null and category != ''">
                and category = #{category}
            </if>
            <if test="keyword != null and keyword != ''">
                and (title like concat('%', #{keyword}, '%') or description like concat('%', #{keyword}, '%') or location like concat('%', #{keyword}, '%'))
            </if>
            order by created_at desc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<LostItem> findPage(@Param("status") String status,
                            @Param("itemType") String itemType,
                            @Param("category") String category,
                            @Param("keyword") String keyword,
                            @Param("offset") int offset,
                            @Param("limit") int limit);

    @Select("""
            <script>
            select count(1) from lost_item
            where 1=1
            <if test="status != null and status != ''">
                and status = #{status}
            </if>
            <if test="itemType != null and itemType != ''">
                and item_type = #{itemType}
            </if>
            <if test="category != null and category != ''">
                and category = #{category}
            </if>
            <if test="keyword != null and keyword != ''">
                and (title like concat('%', #{keyword}, '%') or description like concat('%', #{keyword}, '%') or location like concat('%', #{keyword}, '%'))
            </if>
            </script>
            """)
    long count(@Param("status") String status,
               @Param("itemType") String itemType,
               @Param("category") String category,
               @Param("keyword") String keyword);

    @Select("select * from lost_item where publisher_id = #{publisherId} order by created_at desc")
    List<LostItem> findByPublisherId(@Param("publisherId") Long publisherId);

    @Select("select distinct category from lost_item where category is not null and category != '' order by category")
    List<String> findCategories();

    @Update("update lost_item set status = #{status} where id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Select("select count(1) from lost_item")
    long countAll();

    @Select("select count(1) from lost_item where status = #{status}")
    long countByStatus(@Param("status") String status);

    @Select("select item_type as name, count(1) as value from lost_item group by item_type")
    List<Map<String, Object>> countByType();

    @Select("select category as name, count(1) as value from lost_item where category is not null and category != '' group by category order by value desc limit 8")
    List<Map<String, Object>> countByCategory();
}
