package com.example.lostfound.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.lostfound.domain.entity.FriendRequest;
import com.example.lostfound.mapper.FriendRequestMapper;
import com.example.lostfound.service.FriendRequestService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FriendRequestServiceImpl extends ServiceImpl<FriendRequestMapper, FriendRequest>
        implements FriendRequestService {

    @Override
    public FriendRequest findPending(Long requesterId, Long receiverId) {
        return getOne(Wrappers.<FriendRequest>lambdaQuery()
                .eq(FriendRequest::getRequesterId, requesterId)
                .eq(FriendRequest::getReceiverId, receiverId)
                .eq(FriendRequest::getStatus, "PENDING")
                .last("limit 1"));
    }

    @Override
    public List<FriendRequest> findIncoming(Long receiverId) {
        return list(Wrappers.<FriendRequest>lambdaQuery()
                .eq(FriendRequest::getReceiverId, receiverId)
                .orderByDesc(FriendRequest::getCreatedAt));
    }

    @Override
    public List<FriendRequest> findOutgoing(Long requesterId) {
        return list(Wrappers.<FriendRequest>lambdaQuery()
                .eq(FriendRequest::getRequesterId, requesterId)
                .orderByDesc(FriendRequest::getCreatedAt));
    }
}
