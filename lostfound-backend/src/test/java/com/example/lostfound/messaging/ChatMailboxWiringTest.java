package com.example.lostfound.messaging;

import com.example.lostfound.config.MessagingConfig;
import com.example.lostfound.controller.MessageController;
import com.example.lostfound.mapper.ChatMessageMapper;
import com.example.lostfound.mapper.UserFriendMapper;
import com.example.lostfound.mapper.UserMapper;
import com.example.lostfound.service.ChatMessageService;
import com.example.lostfound.service.UserFriendService;
import com.example.lostfound.service.UserService;
import com.example.lostfound.service.impl.ChatMessageServiceImpl;
import com.example.lostfound.service.impl.UserFriendServiceImpl;
import com.example.lostfound.service.impl.UserServiceImpl;
import com.example.lostfound.websocket.ChatWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/** 校验私信相关 Bean 的装配：存在两个 DirectExchange 时 ChatMailbox 必须拿到私信交换机。 */
class ChatMailboxWiringTest {

    @Test
    void chatBeansAreWired() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory("127.0.0.1", 5672);
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(ConnectionFactory.class, () -> connectionFactory);
        context.registerBean(RabbitTemplate.class, () -> new RabbitTemplate(connectionFactory));
        // Spring Boot 自动配置暴露的 Bean 类型是接口 AmqpAdmin，这里保持一致才能复现真实装配
        context.registerBean(AmqpAdmin.class, () -> new RabbitAdmin(connectionFactory));
        context.registerBean(ObjectMapper.class, () -> new ObjectMapper());
        context.registerBean(UserMapper.class, () -> Mockito.mock(UserMapper.class));
        context.registerBean(UserFriendMapper.class, () -> Mockito.mock(UserFriendMapper.class));
        context.registerBean(ChatMessageMapper.class, () -> Mockito.mock(ChatMessageMapper.class));
        context.register(MessagingConfig.class, ChatMailbox.class, ChatWebSocketHandler.class,
                UserServiceImpl.class, UserFriendServiceImpl.class, ChatMessageServiceImpl.class, MessageController.class);

        context.refresh();
        try {
            ChatMailbox mailbox = context.getBean(ChatMailbox.class);
            assertThat(mailbox.queueName(7L)).isEqualTo("lostfound.chat.user.7");
            assertThat(context.getBean(ChatWebSocketHandler.class)).isNotNull();
            assertThat(context.getBean(MessageController.class)).isNotNull();
            // 控制器只依赖接口，实现类由 impl 包提供
            assertThat(context.getBean(UserService.class)).isInstanceOf(UserServiceImpl.class);
            assertThat(context.getBean(UserFriendService.class)).isInstanceOf(UserFriendServiceImpl.class);
            assertThat(context.getBean(ChatMessageService.class)).isInstanceOf(ChatMessageServiceImpl.class);
        } finally {
            context.close();
        }
    }
}
