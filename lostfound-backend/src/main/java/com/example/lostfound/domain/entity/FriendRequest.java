package com.example.lostfound.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("friend_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequest {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long requesterId;

    private Long receiverId;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;
}
