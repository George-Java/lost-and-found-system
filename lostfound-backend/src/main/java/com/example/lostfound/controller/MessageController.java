package com.example.lostfound.controller;

import com.example.lostfound.auth.AuthContext;
import com.example.lostfound.auth.SessionUser;
import com.example.lostfound.common.ApiResponse;
import com.example.lostfound.common.BusinessException;
import com.example.lostfound.domain.entity.ChatMessage;
import com.example.lostfound.domain.entity.UserAccount;
import com.example.lostfound.messaging.ChatMailbox;
import com.example.lostfound.service.ChatMessageService;
import com.example.lostfound.service.UserFriendService;
import com.example.lostfound.service.UserService;
import com.example.lostfound.domain.dto.ChatMessageRequest;
import com.example.lostfound.domain.vo.ChatMessageResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final UserService userService;
    private final UserFriendService userFriendService;
    private final ChatMessageService chatMessageService;
    private final ChatMailbox chatMailbox;

    /** 发送私信：先落库保证可查历史，再投进接收人队列做在线即时推送。 */
    @PostMapping
    public ApiResponse<ChatMessageResponse> send(@RequestBody ChatMessageRequest request) {
        SessionUser fromUser = AuthContext.get();
        String content = request.content() == null ? "" : request.content().trim();
        if (request.toUserId() == null || !StringUtils.hasText(content)) {
            throw new BusinessException(400, "请选择接收人并输入消息内容");
        }
        if (content.length() > 500) {
            throw new BusinessException(400, "单条消息不能超过 500 个字符");
        }
        if (Objects.equals(fromUser.userId(), request.toUserId())) {
            throw new BusinessException(400, "不能给自己发送消息");
        }
        if (!userFriendService.areFriends(fromUser.userId(), request.toUserId())) {
            throw new BusinessException(403, "只有互为好友后才能发送消息");
        }

        ChatMessage message = new ChatMessage();
        message.setFromUserId(fromUser.userId());
        message.setToUserId(request.toUserId());
        message.setContent(content);
        message.setSentAt(LocalDateTime.now());
        chatMessageService.save(message);

        ChatMessageResponse response = toVO(message, fromUser.username());
        chatMailbox.send(request.toUserId(), response);
        return ApiResponse.success(response);
    }

    /** 与某位好友的历史消息，按时间正序返回。 */
    @GetMapping
    public ApiResponse<List<ChatMessageResponse>> history(@RequestParam Long friendId,
                                                          @RequestParam(defaultValue = "200") int limit) {
        SessionUser currentUser = AuthContext.get();
        UserAccount friend = userService.findById(friendId);
        if (friend == null) {
            throw new BusinessException(404, "用户不存在");
        }
        int size = Math.max(1, Math.min(limit, 500));
        List<ChatMessageResponse> messages = chatMessageService
                .findConversation(currentUser.userId(), friendId, size).stream()
                .map(message -> toVO(message, message.getFromUserId().equals(currentUser.userId())
                        ? currentUser.username()
                        : friend.getUsername()))
                .toList();
        return ApiResponse.success(messages);
    }

    private ChatMessageResponse toVO(ChatMessage message, String fromUsername) {
        return ChatMessageResponse.chat(
                message.getId(),
                message.getFromUserId(),
                fromUsername,
                message.getToUserId(),
                message.getContent(),
                message.getSentAt()
        );
    }
}
