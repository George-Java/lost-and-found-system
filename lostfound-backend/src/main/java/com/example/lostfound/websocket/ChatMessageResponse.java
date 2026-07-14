package com.example.lostfound.websocket;

import java.time.LocalDateTime;

public class ChatMessageResponse {
    private String type = "CHAT";
    private Long fromUserId;
    private String fromUsername;
    private Long toUserId;
    private String content;
    private LocalDateTime sentAt;

    public ChatMessageResponse() {
    }

    public ChatMessageResponse(Long fromUserId, String fromUsername, Long toUserId, String content, LocalDateTime sentAt) {
        this.fromUserId = fromUserId;
        this.fromUsername = fromUsername;
        this.toUserId = toUserId;
        this.content = content;
        this.sentAt = sentAt;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(Long fromUserId) {
        this.fromUserId = fromUserId;
    }

    public String getFromUsername() {
        return fromUsername;
    }

    public void setFromUsername(String fromUsername) {
        this.fromUsername = fromUsername;
    }

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
