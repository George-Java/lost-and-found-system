package com.example.lostfound.websocket;

import com.example.lostfound.auth.SessionUser;
import com.example.lostfound.mapper.UserFriendMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    public static final String SESSION_USER_ATTRIBUTE = "sessionUser";

    private final ObjectMapper objectMapper;
    private final UserFriendMapper userFriendMapper;
    private final Map<Long, Set<WebSocketSession>> onlineSessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ObjectMapper objectMapper, UserFriendMapper userFriendMapper) {
        this.objectMapper = objectMapper;
        this.userFriendMapper = userFriendMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        SessionUser user = currentUser(session);
        if (user == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized"));
            return;
        }
        onlineSessions.computeIfAbsent(user.getUserId(), key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        SessionUser fromUser = currentUser(session);
        if (fromUser == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized"));
            return;
        }

        ChatMessageRequest request;
        try {
            request = objectMapper.readValue(message.getPayload(), ChatMessageRequest.class);
        } catch (IOException ex) {
            sendError(session, "消息格式不正确");
            return;
        }

        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (request.getToUserId() == null || !StringUtils.hasText(content)) {
            sendError(session, "请选择接收人并输入消息内容");
            return;
        }
        if (content.length() > 500) {
            sendError(session, "单条消息不能超过 500 个字符");
            return;
        }
        if (!userFriendMapper.areFriends(fromUser.getUserId(), request.getToUserId())) {
            sendError(session, "只有互为好友后才能发送消息");
            return;
        }

        ChatMessageResponse response = new ChatMessageResponse(
                fromUser.getUserId(),
                fromUser.getUsername(),
                request.getToUserId(),
                content,
                LocalDateTime.now()
        );
        sendToUser(request.getToUserId(), response);
        if (!fromUser.getUserId().equals(request.getToUserId())) {
            sendToUser(fromUser.getUserId(), response);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SessionUser user = currentUser(session);
        if (user == null) {
            return;
        }
        Set<WebSocketSession> sessions = onlineSessions.get(user.getUserId());
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                onlineSessions.remove(user.getUserId());
            }
        }
    }

    private SessionUser currentUser(WebSocketSession session) {
        Object value = session.getAttributes().get(SESSION_USER_ATTRIBUTE);
        return value instanceof SessionUser user ? user : null;
    }

    private void sendToUser(Long userId, ChatMessageResponse response) throws IOException {
        Set<WebSocketSession> sessions = onlineSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        String payload = objectMapper.writeValueAsString(response);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                synchronized (session) {
                    session.sendMessage(new TextMessage(payload));
                }
            }
        }
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        String payload = objectMapper.writeValueAsString(Map.of("type", "ERROR", "message", message));
        if (session.isOpen()) {
            synchronized (session) {
                session.sendMessage(new TextMessage(payload));
            }
        }
    }
}
