package com.example.lostfound.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.lostfound.domain.entity.ChatMessage;

import java.util.List;

public interface ChatMessageService extends IService<ChatMessage> {

    /** 取两人之间最近 limit 条消息，按时间正序返回。 */
    List<ChatMessage> findConversation(Long userId, Long friendId, int limit);
}
