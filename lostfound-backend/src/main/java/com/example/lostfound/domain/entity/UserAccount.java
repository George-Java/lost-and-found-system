package com.example.lostfound.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("user_account")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAccount {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String passwordHash;

    private String realName;

    private String phone;

    private String role;

    private LocalDateTime createdAt;
}
