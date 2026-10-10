package com.example.lostfound.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.lostfound.domain.entity.FriendRequest;

import java.util.List;

public interface FriendRequestService extends IService<FriendRequest> {

    FriendRequest findPending(Long requesterId, Long receiverId);

    List<FriendRequest> findIncoming(Long receiverId);

    List<FriendRequest> findOutgoing(Long requesterId);
}
