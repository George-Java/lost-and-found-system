package com.example.lostfound.messaging;

import com.example.lostfound.common.BusinessException;
import com.example.lostfound.config.MessagingConfig;
import com.example.lostfound.domain.vo.ChatMessageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 异步私信信箱：每个用户一个持久化队列，路由键就是队列名。
 * 接收人在线时由消费者的 WebSocket 会话即时取走；离线时消息留在队列里，等其上线后立即送达。
 */
@Component
public class ChatMailbox {

    private static final Logger log = LoggerFactory.getLogger(ChatMailbox.class);

    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;
    private final ConnectionFactory connectionFactory;
    private final DirectExchange chatExchange;
    private final ObjectMapper objectMapper;
    /** 已声明过的信箱，避免每条消息都去声明一次队列。 */
    private final Set<Long> readyMailboxes = ConcurrentHashMap.newKeySet();
    /** 在线用户对应的消费者，用户全部掉线后停止消费，消息重新堆积在队列中。 */
    private final Map<Long, SimpleMessageListenerContainer> consumers = new ConcurrentHashMap<>();

    public ChatMailbox(RabbitTemplate rabbitTemplate,
                       AmqpAdmin amqpAdmin,
                       ConnectionFactory connectionFactory,
                       DirectExchange chatExchange,
                       ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.amqpAdmin = amqpAdmin;
        this.connectionFactory = connectionFactory;
        this.chatExchange = chatExchange;
        this.objectMapper = objectMapper;
    }

    public String queueName(Long userId) {
        return MessagingConfig.CHAT_QUEUE_PREFIX + userId;
    }

    /** 投递到接收人的信箱队列，不依赖接收人是否在线。 */
    public void send(Long toUserId, ChatMessageResponse message) {
        declareMailbox(toUserId);
        rabbitTemplate.convertAndSend(MessagingConfig.CHAT_EXCHANGE, queueName(toUserId), toJson(message));
    }

    /** 用户上线：先清掉队列里的积压（历史以数据库为准），再启动消费者接收后续消息。 */
    public void subscribe(Long userId, Consumer<ChatMessageResponse> handler) {
        consumers.computeIfAbsent(userId, id -> startConsumer(id, handler));
    }

    /** 用户全部下线：停止消费，后续消息继续留在队列中。 */
    public void unsubscribe(Long userId) {
        SimpleMessageListenerContainer container = consumers.remove(userId);
        if (container != null) {
            container.stop();
        }
    }

    @PreDestroy
    public void shutdown() {
        consumers.values().forEach(SimpleMessageListenerContainer::stop);
        consumers.clear();
    }

    private SimpleMessageListenerContainer startConsumer(Long userId, Consumer<ChatMessageResponse> handler) {
        declareMailbox(userId);
        amqpAdmin.purgeQueue(queueName(userId));
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueueNames(queueName(userId));
        container.setMissingQueuesFatal(false);
        container.setMessageListener(message -> {
            ChatMessageResponse payload = fromJson(new String(message.getBody(), StandardCharsets.UTF_8));
            if (payload != null) {
                handler.accept(payload);
            }
        });
        container.start();
        return container;
    }

    private void declareMailbox(Long userId) {
        if (readyMailboxes.contains(userId)) {
            return;
        }
        Queue mailbox = QueueBuilder.durable(queueName(userId)).build();
        amqpAdmin.declareQueue(mailbox);
        amqpAdmin.declareBinding(BindingBuilder.bind(mailbox).to(chatExchange).with(queueName(userId)));
        readyMailboxes.add(userId);
    }

    private String toJson(ChatMessageResponse message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "消息序列化失败");
        }
    }

    private ChatMessageResponse fromJson(String body) {
        try {
            return objectMapper.readValue(body, ChatMessageResponse.class);
        } catch (IOException ex) {
            log.warn("丢弃无法解析的私信消息: {}", body);
            return null;
        }
    }
}
