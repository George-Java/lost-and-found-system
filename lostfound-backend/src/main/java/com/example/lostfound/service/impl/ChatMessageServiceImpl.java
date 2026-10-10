package com.example.lostfound.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.lostfound.domain.entity.ChatMessage;
import com.example.lostfound.mapper.ChatMessageMapper;
import com.example.lostfound.service.ChatMessageService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {

    @Override
    public List<ChatMessage> findConversation(Long userId, Long friendId, int limit) {
        List<ChatMessage> messages = list(Wrappers.<ChatMessage>lambdaQuery()
                .and(w -> w.eq(ChatMessage::getFromUserId, userId).eq(ChatMessage::getToUserId, friendId))
                .or(w -> w.eq(ChatMessage::getFromUserId, friendId).eq(ChatMessage::getToUserId, userId))
                .orderByDesc(ChatMessage::getSentAt)
                .orderByDesc(ChatMessage::getId)
                .last("limit " + limit));
        Collections.reverse(messages);
        return messages;
    }
}
