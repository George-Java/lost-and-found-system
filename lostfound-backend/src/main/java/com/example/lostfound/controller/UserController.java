package com.example.lostfound.controller;

import com.example.lostfound.auth.AuthContext;
import com.example.lostfound.auth.LoginRequired;
import com.example.lostfound.common.ApiResponse;
import com.example.lostfound.common.BusinessException;
import com.example.lostfound.dto.FriendRequestCreateRequest;
import com.example.lostfound.dto.FriendRequestReviewRequest;
import com.example.lostfound.entity.FriendRequest;
import com.example.lostfound.mapper.UserMapper;
import com.example.lostfound.mapper.FriendRequestMapper;
import com.example.lostfound.mapper.UserFriendMapper;
import com.example.lostfound.entity.UserAccount;
import com.example.lostfound.entity.UserFriend;
import com.example.lostfound.vo.FriendRequestVO;
import com.example.lostfound.vo.UserSearchVO;
import com.example.lostfound.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@LoginRequired
public class UserController {

    private final UserMapper userMapper;
    private final UserFriendMapper userFriendMapper;
    private final FriendRequestMapper friendRequestMapper;

    public UserController(UserMapper userMapper,
                          UserFriendMapper userFriendMapper,
                          FriendRequestMapper friendRequestMapper) {
        this.userMapper = userMapper;
        this.userFriendMapper = userFriendMapper;
        this.friendRequestMapper = friendRequestMapper;
    }

    @GetMapping("/contacts")
    public ApiResponse<List<UserVO>> contacts() {
        Long currentUserId = AuthContext.userId();
        List<UserVO> users = userFriendMapper.findByUserId(currentUserId).stream()
                .map(UserFriend::getFriendId)
                .map(userMapper::findById)
                .filter(Objects::nonNull)
                .map(this::toUserVO)
                .toList();
        return ApiResponse.success(users);
    }

    @GetMapping("/search")
    public ApiResponse<List<UserSearchVO>> search(@RequestParam String keyword) {
        String trimmed = keyword == null ? "" : keyword.trim();
        if (trimmed.length() < 2) {
            throw new BusinessException(400, "Search keyword must be at least 2 characters");
        }
        Long currentUserId = AuthContext.userId();
        List<UserSearchVO> users = userMapper.search(trimmed).stream()
                .filter(user -> !Objects.equals(user.getId(), currentUserId))
                .map(user -> new UserSearchVO(
                        user.getId(),
                        user.getUsername(),
                        user.getRealName(),
                        user.getPhone(),
                        user.getRole(),
                        friendshipStatus(currentUserId, user.getId())
                ))
                .toList();
        return ApiResponse.success(users);
    }

    @PostMapping("/friend-requests")
    public ApiResponse<FriendRequestVO> createFriendRequest(@RequestBody @Valid FriendRequestCreateRequest request) {
        Long currentUserId = AuthContext.userId();
        Long targetUserId = request.getTargetUserId();
        if (Objects.equals(currentUserId, targetUserId)) {
            throw new BusinessException(400, "Cannot add yourself as friend");
        }
        UserAccount target = userMapper.findById(targetUserId);
        if (target == null) {
            throw new BusinessException(404, "Target user not found");
        }
        if (userFriendMapper.areFriends(currentUserId, targetUserId)) {
            throw new BusinessException(409, "You are already friends");
        }
        if (friendRequestMapper.findPending(currentUserId, targetUserId) != null) {
            throw new BusinessException(409, "Friend request already sent");
        }
        FriendRequest reverse = friendRequestMapper.findPending(targetUserId, currentUserId);
        if (reverse != null) {
            throw new BusinessException(409, "This user has already sent you a pending friend request");
        }

        FriendRequest requestEntity = new FriendRequest();
        requestEntity.setRequesterId(currentUserId);
        requestEntity.setReceiverId(targetUserId);
        requestEntity.setStatus("PENDING");
        friendRequestMapper.insert(requestEntity);
        return ApiResponse.success(toFriendRequestVO(requestEntity));
    }

    @GetMapping("/friend-requests/incoming")
    public ApiResponse<List<FriendRequestVO>> incomingRequests() {
        return ApiResponse.success(friendRequestMapper.findIncoming(AuthContext.userId()).stream()
                .map(this::toFriendRequestVO)
                .toList());
    }

    @GetMapping("/friend-requests/outgoing")
    public ApiResponse<List<FriendRequestVO>> outgoingRequests() {
        return ApiResponse.success(friendRequestMapper.findOutgoing(AuthContext.userId()).stream()
                .map(this::toFriendRequestVO)
                .toList());
    }

    @PutMapping("/friend-requests/{id}/review")
    public ApiResponse<Void> reviewFriendRequest(@PathVariable Long id,
                                                 @RequestBody @Valid FriendRequestReviewRequest request) {
        FriendRequest friendRequest = friendRequestMapper.selectById(id);
        if (friendRequest == null) {
            throw new BusinessException(404, "Friend request not found");
        }
        if (!Objects.equals(friendRequest.getReceiverId(), AuthContext.userId())) {
            throw new BusinessException(403, "No permission to review this friend request");
        }
        if (!"PENDING".equals(friendRequest.getStatus())) {
            throw new BusinessException(400, "Friend request has already been reviewed");
        }

        String status = normalizeFriendRequestStatus(request.getStatus());
        friendRequest.setStatus(status);
        friendRequest.setReviewedAt(LocalDateTime.now());
        friendRequestMapper.updateById(friendRequest);
        if ("APPROVED".equals(status)) {
            userFriendMapper.addFriendPair(friendRequest.getRequesterId(), friendRequest.getReceiverId());
        }
        return ApiResponse.success();
    }

    private String friendshipStatus(Long currentUserId, Long targetUserId) {
        if (userFriendMapper.areFriends(currentUserId, targetUserId)) {
            return "FRIEND";
        }
        if (friendRequestMapper.findPending(currentUserId, targetUserId) != null) {
            return "REQUEST_SENT";
        }
        if (friendRequestMapper.findPending(targetUserId, currentUserId) != null) {
            return "REQUEST_RECEIVED";
        }
        return "NONE";
    }

    private FriendRequestVO toFriendRequestVO(FriendRequest friendRequest) {
        return new FriendRequestVO(
                friendRequest.getId(),
                Optional.ofNullable(userMapper.findById(friendRequest.getRequesterId())).map(this::toUserVO).orElse(null),
                Optional.ofNullable(userMapper.findById(friendRequest.getReceiverId())).map(this::toUserVO).orElse(null),
                friendRequest.getStatus(),
                friendRequest.getCreatedAt(),
                friendRequest.getReviewedAt()
        );
    }

    private UserVO toUserVO(UserAccount user) {
        return new UserVO(user.getId(), user.getUsername(), user.getRealName(), user.getPhone(), user.getRole());
    }

    private String normalizeFriendRequestStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!"APPROVED".equals(normalized) && !"REJECTED".equals(normalized)) {
            throw new BusinessException(400, "Status must be APPROVED or REJECTED");
        }
        return normalized;
    }
}
