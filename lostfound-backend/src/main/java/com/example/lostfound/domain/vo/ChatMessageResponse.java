package com.example.lostfound.domain.vo;

import java.time.LocalDateTime;

/** 私信内容：既是消息队列里的推送载荷，也是前端渲染的消息体，id 用于历史与推送去重。 */
public record ChatMessageResponse(
        String type,
        Long id,
        Long fromUserId,
        String fromUsername,
        Long toUserId,
        String content,
        LocalDateTime sentAt) {

    /** 只保留一个构造器，避免 Jackson 反序列化时挑不出来；type 统一由这里补上。 */
    public ChatMessageResponse {
        if (type == null || type.isBlank()) {
            type = "CHAT";
        }
    }

    public static ChatMessageResponse chat(Long id, Long fromUserId, String fromUsername,
                                           Long toUserId, String content, LocalDateTime sentAt) {
        return new ChatMessageResponse("CHAT", id, fromUserId, fromUsername, toUserId, content, sentAt);
    }
}
