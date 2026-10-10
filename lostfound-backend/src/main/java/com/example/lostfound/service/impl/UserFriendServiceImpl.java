package com.example.lostfound.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.lostfound.domain.entity.UserFriend;
import com.example.lostfound.mapper.UserFriendMapper;
import com.example.lostfound.service.UserFriendService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserFriendServiceImpl extends ServiceImpl<UserFriendMapper, UserFriend> implements UserFriendService {

    @Override
    public boolean areFriends(Long userId, Long friendId) {
        if (userId == null || friendId == null) {
            return false;
        }
        return count(Wrappers.<UserFriend>lambdaQuery()
                .eq(UserFriend::getUserId, userId)
                .eq(UserFriend::getFriendId, friendId)) > 0;
    }

    @Override
    public List<UserFriend> findByUserId(Long userId) {
        return list(Wrappers.<UserFriend>lambdaQuery()
                .eq(UserFriend::getUserId, userId)
                .orderByDesc(UserFriend::getCreatedAt));
    }

    @Override
    public void addFriendPair(Long userId, Long friendId) {
        if (!areFriends(userId, friendId)) {
            UserFriend first = new UserFriend();
            first.setUserId(userId);
            first.setFriendId(friendId);
            save(first);
        }
        if (!areFriends(friendId, userId)) {
            UserFriend second = new UserFriend();
            second.setUserId(friendId);
            second.setFriendId(userId);
            save(second);
        }
    }

    @Override
    public void deleteFriendPair(Long userId, Long friendId) {
        remove(Wrappers.<UserFriend>lambdaQuery()
                .eq(UserFriend::getUserId, userId)
                .eq(UserFriend::getFriendId, friendId));
        remove(Wrappers.<UserFriend>lambdaQuery()
                .eq(UserFriend::getUserId, friendId)
                .eq(UserFriend::getFriendId, userId));
    }
}
