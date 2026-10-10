package com.example.lostfound.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 私信记录，落库后才能刷新页面、重新登录查看历史。 */
@TableName("chat_message")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fromUserId;

    private Long toUserId;

    private String content;

    private LocalDateTime sentAt;
}
