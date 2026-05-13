package com.example.lostfound.vo;

public class UserVO {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String role;

    public UserVO() {
    }

    public UserVO(Long id, String username, String realName, String phone, String role) {
        this.id = id;
        this.username = username;
        this.realName = realName;
        this.phone = phone;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
