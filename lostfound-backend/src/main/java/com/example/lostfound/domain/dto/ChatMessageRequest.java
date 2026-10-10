package com.example.lostfound.domain.dto;

/** 发送私信的请求体（POST /api/messages）。 */
public record ChatMessageRequest(Long toUserId, String content) {
}
