package com.example.lostfound.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("user_friend")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFriend {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long friendId;

    private LocalDateTime createdAt;
}
