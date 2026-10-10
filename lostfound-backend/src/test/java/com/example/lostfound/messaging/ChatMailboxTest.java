package com.example.lostfound.messaging;

import com.example.lostfound.config.MessagingConfig;
import com.example.lostfound.domain.vo.ChatMessageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 需要真实 RabbitMQ 的集成测试：设置 CHAT_MQ_IT=true 后执行，默认连 192.168.68.128:5672（可用 SPRING_RABBITMQ_* 覆盖）。
 * 验证队列作为推送通道的行为：离线堆积、上线清空积压、在线实时推送、下线后重新堆积。
 */
@EnabledIfEnvironmentVariable(named = "CHAT_MQ_IT", matches = "true")
class ChatMailboxTest {

    private CachingConnectionFactory connectionFactory;
    private RabbitAdmin rabbitAdmin;
    private ChatMailbox mailbox;
    private Long userId;

    @BeforeEach
    void setUp() {
        String host = env("SPRING_RABBITMQ_HOST", "192.168.68.128");
        int port = Integer.parseInt(env("SPRING_RABBITMQ_PORT", "5672"));
        connectionFactory = new CachingConnectionFactory(host, port);
        connectionFactory.setUsername(env("SPRING_RABBITMQ_USERNAME", "root"));
        connectionFactory.setPassword(env("SPRING_RABBITMQ_PASSWORD", "123456"));

        rabbitAdmin = new RabbitAdmin(connectionFactory);
        DirectExchange exchange = new DirectExchange(MessagingConfig.CHAT_EXCHANGE, true, false);
        rabbitAdmin.declareExchange(exchange);

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mailbox = new ChatMailbox(new RabbitTemplate(connectionFactory), rabbitAdmin, connectionFactory, exchange, objectMapper);
        userId = 900_000L + ThreadLocalRandom.current().nextInt(100_000);
    }

    @AfterEach
    void tearDown() {
        mailbox.shutdown();
        rabbitAdmin.deleteQueue(mailbox.queueName(userId));
        connectionFactory.destroy();
    }

    @Test
    void offlineMessagesQueueUpAndOnlineReceiverGetsPushed() throws Exception {
        // 接收人不在线：新消息先堆在队列里，作为上线前的临时推送缓冲（历史以数据库为准）
        mailbox.send(userId, message("第一条"));
        mailbox.send(userId, message("第二条"));
        Thread.sleep(500);
        assertThat(queueMessageCount()).isEqualTo(2);

        // 上线：清掉积压，之后的消息实时推送
        List<ChatMessageResponse> received = new CopyOnWriteArrayList<>();
        mailbox.subscribe(userId, received::add);
        Thread.sleep(1000);
        assertThat(received).isEmpty();
        assertThat(queueMessageCount()).isZero();

        mailbox.send(userId, message("第三条"));
        awaitSize(received, 1);
        assertThat(received.get(0).content()).isEqualTo("第三条");

        // 下线后消息重新堆积，等待下次上线推送
        mailbox.unsubscribe(userId);
        mailbox.send(userId, message("第四条"));
        Thread.sleep(500);
        assertThat(queueMessageCount()).isEqualTo(1);
    }

    private ChatMessageResponse message(String content) {
        return ChatMessageResponse.chat(null, userId, "tester", userId, content, LocalDateTime.now());
    }

    private int queueMessageCount() {
        Properties properties = rabbitAdmin.getQueueProperties(mailbox.queueName(userId));
        return (Integer) properties.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
    }

    private void awaitSize(List<ChatMessageResponse> received, int size) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (received.size() < size && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }
        assertThat(received).hasSize(size);
    }

    private String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
