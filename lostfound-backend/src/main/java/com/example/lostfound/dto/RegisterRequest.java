package com.example.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 20, message = "Username length must be 4-20")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 30, message = "Password length must be 6-30")
    private String password;

    @NotBlank(message = "Real name is required")
    private String realName;

    private String phone;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
}
