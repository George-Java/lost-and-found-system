package com.example.lostfound.vo;

public class LoginVO {
    private String token;
    private Long userId;
    private String username;
    private String role;
    private String realName;

    public LoginVO() {
    }

    public LoginVO(String token, Long userId, String username, String role, String realName) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.realName = realName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }
}
