package com.example.lostfound.domain.vo;

import java.time.LocalDateTime;

public record FriendRequestVO(Long id, UserVO requester, UserVO receiver, String status,
                             LocalDateTime createdAt, LocalDateTime reviewedAt) {
}
