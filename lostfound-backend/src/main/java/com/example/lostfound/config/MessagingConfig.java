package com.example.lostfound.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {
    public static final String AUDIT_EXCHANGE = "lostfound.audit.exchange";
    public static final String AUDIT_QUEUE = "lostfound.audit.queue";
    public static final String AUDIT_ROUTING_KEY = "audit.event";

    /** 私信交换机：每个用户绑定一个持久化队列充当"离线信箱"，路由键即队列名。 */
    public static final String CHAT_EXCHANGE = "lostfound.chat.exchange";
    public static final String CHAT_QUEUE_PREFIX = "lostfound.chat.user.";

    @Bean
    DirectExchange auditExchange() { return new DirectExchange(AUDIT_EXCHANGE, true, false); }

    @Bean
    Queue auditQueue() { return new Queue(AUDIT_QUEUE, true); }

    @Bean
    Binding auditBinding(Queue auditQueue, DirectExchange auditExchange) {
        return BindingBuilder.bind(auditQueue).to(auditExchange).with(AUDIT_ROUTING_KEY);
    }

    @Bean
    DirectExchange chatExchange() { return new DirectExchange(CHAT_EXCHANGE, true, false); }
}
