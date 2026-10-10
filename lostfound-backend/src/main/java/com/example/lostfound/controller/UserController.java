package com.example.lostfound.controller;

import com.example.lostfound.auth.AuthContext;
import com.example.lostfound.common.ApiResponse;
import com.example.lostfound.common.BusinessException;
import com.example.lostfound.domain.dto.FriendRequestCreateRequest;
import com.example.lostfound.domain.dto.FriendRequestReviewRequest;
import com.example.lostfound.domain.entity.FriendRequest;
import com.example.lostfound.domain.entity.UserAccount;
import com.example.lostfound.domain.entity.UserFriend;
import com.example.lostfound.service.FriendRequestService;
import com.example.lostfound.service.UserFriendService;
import com.example.lostfound.service.UserService;
import com.example.lostfound.domain.vo.FriendRequestVO;
import com.example.lostfound.domain.vo.UserSearchVO;
import com.example.lostfound.domain.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserFriendService userFriendService;
    private final FriendRequestService friendRequestService;

    @GetMapping("/contacts")
    public ApiResponse<List<UserVO>> contacts() {
        Long currentUserId = AuthContext.userId();
        List<UserVO> users = userFriendService.findByUserId(currentUserId).stream()
                .map(UserFriend::getFriendId)
                .map(userService::findById)
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
        List<UserSearchVO> users = userService.search(trimmed).stream()
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
        Long targetUserId = request.targetUserId();
        if (Objects.equals(currentUserId, targetUserId)) {
            throw new BusinessException(400, "Cannot add yourself as friend");
        }
        UserAccount target = userService.findById(targetUserId);
        if (target == null) {
            throw new BusinessException(404, "Target user not found");
        }
        if (userFriendService.areFriends(currentUserId, targetUserId)) {
            throw new BusinessException(409, "You are already friends");
        }
        if (friendRequestService.findPending(currentUserId, targetUserId) != null) {
            throw new BusinessException(409, "Friend request already sent");
        }
        FriendRequest reverse = friendRequestService.findPending(targetUserId, currentUserId);
        if (reverse != null) {
            throw new BusinessException(409, "This user has already sent you a pending friend request");
        }

        FriendRequest requestEntity = new FriendRequest();
        requestEntity.setRequesterId(currentUserId);
        requestEntity.setReceiverId(targetUserId);
        requestEntity.setStatus("PENDING");
        friendRequestService.save(requestEntity);
        return ApiResponse.success(toFriendRequestVO(requestEntity));
    }

    @GetMapping("/friend-requests/incoming")
    public ApiResponse<List<FriendRequestVO>> incomingRequests() {
        return ApiResponse.success(friendRequestService.findIncoming(AuthContext.userId()).stream()
                .map(this::toFriendRequestVO)
                .toList());
    }

    @GetMapping("/friend-requests/outgoing")
    public ApiResponse<List<FriendRequestVO>> outgoingRequests() {
        return ApiResponse.success(friendRequestService.findOutgoing(AuthContext.userId()).stream()
                .map(this::toFriendRequestVO)
                .toList());
    }

    @PutMapping("/friend-requests/{id}/review")
    public ApiResponse<Void> reviewFriendRequest(@PathVariable Long id,
                                                 @RequestBody @Valid FriendRequestReviewRequest request) {
        FriendRequest friendRequest = friendRequestService.getById(id);
        if (friendRequest == null) {
            throw new BusinessException(404, "Friend request not found");
        }
        if (!Objects.equals(friendRequest.getReceiverId(), AuthContext.userId())) {
            throw new BusinessException(403, "No permission to review this friend request");
        }
        if (!"PENDING".equals(friendRequest.getStatus())) {
            throw new BusinessException(400, "Friend request has already been reviewed");
        }

        String status = normalizeFriendRequestStatus(request.status());
        friendRequest.setStatus(status);
        friendRequest.setReviewedAt(LocalDateTime.now());
        friendRequestService.updateById(friendRequest);
        if ("APPROVED".equals(status)) {
            userFriendService.addFriendPair(friendRequest.getRequesterId(), friendRequest.getReceiverId());
        }
        return ApiResponse.success();
    }

    @DeleteMapping("/friends/{friendId}")
    public ApiResponse<Void> deleteFriend(@PathVariable Long friendId) {
        Long currentUserId = AuthContext.userId();
        if (Objects.equals(currentUserId, friendId)) {
            throw new BusinessException(400, "Cannot delete yourself");
        }
        if (!userFriendService.areFriends(currentUserId, friendId)) {
            throw new BusinessException(404, "Friend relationship not found");
        }
        userFriendService.deleteFriendPair(currentUserId, friendId);
        return ApiResponse.success();
    }

    private String friendshipStatus(Long currentUserId, Long targetUserId) {
        if (userFriendService.areFriends(currentUserId, targetUserId)) {
            return "FRIEND";
        }
        if (friendRequestService.findPending(currentUserId, targetUserId) != null) {
            return "REQUEST_SENT";
        }
        if (friendRequestService.findPending(targetUserId, currentUserId) != null) {
            return "REQUEST_RECEIVED";
        }
        return "NONE";
    }

    private FriendRequestVO toFriendRequestVO(FriendRequest friendRequest) {
        return new FriendRequestVO(
                friendRequest.getId(),
                Optional.ofNullable(userService.findById(friendRequest.getRequesterId())).map(this::toUserVO).orElse(null),
                Optional.ofNullable(userService.findById(friendRequest.getReceiverId())).map(this::toUserVO).orElse(null),
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
