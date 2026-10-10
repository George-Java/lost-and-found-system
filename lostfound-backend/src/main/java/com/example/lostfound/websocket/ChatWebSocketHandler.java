package com.example.lostfound.websocket;

import com.example.lostfound.auth.SessionUser;
import com.example.lostfound.domain.vo.ChatMessageResponse;
import com.example.lostfound.messaging.ChatMailbox;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 只负责把消息队列里的私信推给在线的浏览器，不再处理发送。
 * 用户上线即开始消费自己的信箱队列，离线期间积压的消息会一次性送达。
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    public static final String SESSION_USER_ATTRIBUTE = "sessionUser";

    private final ObjectMapper objectMapper;
    private final ChatMailbox chatMailbox;
    private final Map<Long, Set<WebSocketSession>> onlineSessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ObjectMapper objectMapper, ChatMailbox chatMailbox) {
        this.objectMapper = objectMapper;
        this.chatMailbox = chatMailbox;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        SessionUser user = currentUser(session);
        if (user == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized"));
            return;
        }
        onlineSessions.computeIfAbsent(user.userId(), key -> ConcurrentHashMap.newKeySet()).add(session);
        chatMailbox.subscribe(user.userId(), message -> push(user.userId(), message));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SessionUser user = currentUser(session);
        if (user == null) {
            return;
        }
        Set<WebSocketSession> sessions = onlineSessions.get(user.userId());
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (!sessions.isEmpty()) {
            return;
        }
        onlineSessions.remove(user.userId(), sessions);
        chatMailbox.unsubscribe(user.userId());
    }

    private SessionUser currentUser(WebSocketSession session) {
        Object value = session.getAttributes().get(SESSION_USER_ATTRIBUTE);
        return value instanceof SessionUser user ? user : null;
    }

    private void push(Long userId, ChatMessageResponse message) {
        Set<WebSocketSession> sessions = onlineSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        String payload;
        try {
            payload = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException ex) {
            log.warn("私信序列化失败: {}", message.content(), ex);
            return;
        }

        TextMessage text = new TextMessage(payload);
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(text);
                    }
                } else {
                    sessions.remove(session);
                }
            } catch (IOException ex) {
                log.warn("私信推送失败, userId={}", userId, ex);
                sessions.remove(session);
            }
        }
    }
}
