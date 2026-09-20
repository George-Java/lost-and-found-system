package com.example.lostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("lost_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LostItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;

    @TableField("image_urls")
    private String imageUrls;
    private String category;
    private String location;

    @TableField("lost_time")
    private LocalDateTime lostTime;
    private String contact;

    @TableField("item_type")
    private String itemType;

    @TableField("publisher_id")
    private Long publisherId;
    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
