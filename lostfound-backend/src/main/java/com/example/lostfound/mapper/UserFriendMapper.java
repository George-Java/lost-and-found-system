package com.example.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.lostfound.entity.UserFriend;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserFriendMapper extends BaseMapper<UserFriend> {

    default boolean areFriends(Long userId, Long friendId) {
        if (userId == null || friendId == null) {
            return false;
        }
        return selectCount(Wrappers.<UserFriend>lambdaQuery()
                .eq(UserFriend::getUserId, userId)
                .eq(UserFriend::getFriendId, friendId)) > 0;
    }

    default List<UserFriend> findByUserId(Long userId) {
        return selectList(Wrappers.<UserFriend>lambdaQuery()
                .eq(UserFriend::getUserId, userId)
                .orderByDesc(UserFriend::getCreatedAt));
    }

    default void addFriendPair(Long userId, Long friendId) {
        if (!areFriends(userId, friendId)) {
            UserFriend first = new UserFriend();
            first.setUserId(userId);
            first.setFriendId(friendId);
            insert(first);
        }
        if (!areFriends(friendId, userId)) {
            UserFriend second = new UserFriend();
            second.setUserId(friendId);
            second.setFriendId(userId);
            insert(second);
        }
    }
}
