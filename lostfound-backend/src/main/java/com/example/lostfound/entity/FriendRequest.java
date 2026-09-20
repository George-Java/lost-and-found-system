package com.example.lostfound.entity;

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

    @TableField("requester_id")
    private Long requesterId;

    @TableField("receiver_id")
    private Long receiverId;

    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;
}
