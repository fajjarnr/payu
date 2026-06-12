package id.payu.integration.adapter.messaging;

import id.payu.integration.application.port.out.MessagePublisherPort;
import id.payu.integration.domain.model.IntegrationMessage;
import id.payu.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter for publishing messages to external systems.
 * Implements MessagePublisherPort for Kafka, gRPC, and HTTP.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessagePublisherAdapter implements MessagePublisherPort {

    private final OutboxService outboxService;
    private final RestTemplate restTemplate;

    @Override
    public void publishToKafka(String topic, IntegrationMessage message) {
        log.debug("Publishing message {} to Kafka topic via Outbox: {}", message.getMessageId(), topic);
        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("messageId", message.getMessageId());
            payloadMap.put("type", message.getType());
            payloadMap.put("status", message.getStatus());
            payloadMap.put("payload", message.getTransformedPayload() != null ? message.getTransformedPayload() : message.getRawPayload());

            outboxService.createEvent(
                "IntegrationMessage",
                message.getMessageId(),
                message.getType() != null ? message.getType().name() : "IntegrationMessagePublished",
                payloadMap,
                null,
                topic
            );
            log.info("Created outbox event for integration message: {}", message.getMessageId());
        } catch (Exception e) {
            log.error("Error publishing integration message to outbox", e);
            throw new MessagePublishException("Outbox publish failed", e);
        }
    }

    @Override
    public String publishToGrpc(String serviceName, String operation, String payload) {
        log.debug("Publishing to gRPC service: {} operation: {}", serviceName, operation);
        // gRPC implementation would use gRPC client stub
        // For now, return a placeholder
        return "gRPC response placeholder";
    }

    @Override
    public String sendHttp(String url, String payload) {
        log.debug("Sending HTTP request to: {}", url);
        try {
            return restTemplate.postForObject(url, payload, String.class);
        } catch (Exception e) {
            log.error("HTTP request failed", e);
            throw new MessagePublishException("HTTP request failed", e);
        }
    }

    /**
     * Exception for message publishing errors.
     */
    public static class MessagePublishException extends RuntimeException {
        public MessagePublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
