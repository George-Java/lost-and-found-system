package com.example.lostfound.vo;

import java.time.LocalDateTime;

public class FriendRequestVO {
    private Long id;
    private UserVO requester;
    private UserVO receiver;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    public FriendRequestVO() {
    }

    public FriendRequestVO(Long id, UserVO requester, UserVO receiver, String status, LocalDateTime createdAt, LocalDateTime reviewedAt) {
        this.id = id;
        this.requester = requester;
        this.receiver = receiver;
        this.status = status;
        this.createdAt = createdAt;
        this.reviewedAt = reviewedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserVO getRequester() {
        return requester;
    }

    public void setRequester(UserVO requester) {
        this.requester = requester;
    }

    public UserVO getReceiver() {
        return receiver;
    }

    public void setReceiver(UserVO receiver) {
        this.receiver = receiver;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
