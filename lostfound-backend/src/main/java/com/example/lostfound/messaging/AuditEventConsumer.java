package com.example.lostfound.messaging;

import com.example.lostfound.config.MessagingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Extension point for email/SMS/in-app notifications; currently retains a durable operational audit trail in logs. */
@Component
public class AuditEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

    @RabbitListener(queues = MessagingConfig.AUDIT_QUEUE)
    public void consume(Map<String, Object> event) {
        log.info("AUDIT_EVENT {}", event);
    }
}
