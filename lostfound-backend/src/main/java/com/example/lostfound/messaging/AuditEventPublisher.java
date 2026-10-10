package com.example.lostfound.messaging;

import com.example.lostfound.config.MessagingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;

/** Non-critical events are asynchronous: failures never undo completed core business transactions. */
@Component
public class AuditEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(AuditEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public AuditEventPublisher(RabbitTemplate rabbitTemplate) { this.rabbitTemplate = rabbitTemplate; }

    public void publish(String eventType, Long operatorId, Long resourceId) {
        Map<String, Object> event = Map.of(
                "eventType", eventType, "operatorId", operatorId, "resourceId", resourceId,
                "occurredAt", OffsetDateTime.now().toString());
        try {
            rabbitTemplate.convertAndSend(MessagingConfig.AUDIT_EXCHANGE, MessagingConfig.AUDIT_ROUTING_KEY, event);
        } catch (RuntimeException ex) {
            log.error("Audit event publish failed, type={}, resource={}", eventType, resourceId, ex);
        }
    }
}
