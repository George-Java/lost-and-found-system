package com.example.lostfound.vo;

public class UserSearchVO extends UserVO {
    private String friendshipStatus;

    public UserSearchVO() {
    }

    public UserSearchVO(Long id, String username, String realName, String phone, String role, String friendshipStatus) {
        super(id, username, realName, phone, role);
        this.friendshipStatus = friendshipStatus;
    }

    public String getFriendshipStatus() {
        return friendshipStatus;
    }

    public void setFriendshipStatus(String friendshipStatus) {
        this.friendshipStatus = friendshipStatus;
    }
}
