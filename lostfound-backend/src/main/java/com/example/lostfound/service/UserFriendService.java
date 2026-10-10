package com.example.lostfound.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.lostfound.domain.entity.UserFriend;

import java.util.List;

public interface UserFriendService extends IService<UserFriend> {

    boolean areFriends(Long userId, Long friendId);

    List<UserFriend> findByUserId(Long userId);

    /** 好友关系是双向的，同意申请时一次写两条。 */
    void addFriendPair(Long userId, Long friendId);

    void deleteFriendPair(Long userId, Long friendId);
}
