package com.example.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.lostfound.entity.FriendRequest;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FriendRequestMapper extends BaseMapper<FriendRequest> {

    default FriendRequest findPending(Long requesterId, Long receiverId) {
        return selectOne(Wrappers.<FriendRequest>lambdaQuery()
                .eq(FriendRequest::getRequesterId, requesterId)
                .eq(FriendRequest::getReceiverId, receiverId)
                .eq(FriendRequest::getStatus, "PENDING")
                .last("limit 1"));
    }

    default List<FriendRequest> findIncoming(Long receiverId) {
        return selectList(Wrappers.<FriendRequest>lambdaQuery()
                .eq(FriendRequest::getReceiverId, receiverId)
                .orderByDesc(FriendRequest::getCreatedAt));
    }

    default List<FriendRequest> findOutgoing(Long requesterId) {
        return selectList(Wrappers.<FriendRequest>lambdaQuery()
                .eq(FriendRequest::getRequesterId, requesterId)
                .orderByDesc(FriendRequest::getCreatedAt));
    }
}
