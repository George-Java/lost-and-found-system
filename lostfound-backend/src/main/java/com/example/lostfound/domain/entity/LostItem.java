package com.example.lostfound.domain.entity;

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

    private String imageUrls;
    private String category;
    private String location;

    private LocalDateTime lostTime;
    private String contact;

    private String itemType;

    private Long publisherId;
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
