package com.example.lostfound.domain.vo;

public record LoginVO(String token, Long userId, String username, String role, String realName) {
}
