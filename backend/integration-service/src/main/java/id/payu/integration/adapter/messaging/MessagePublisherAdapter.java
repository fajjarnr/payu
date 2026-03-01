package id.payu.integration.adapter.messaging;

import id.payu.integration.application.port.out.MessagePublisherPort;
import id.payu.integration.domain.model.IntegrationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Adapter for publishing messages to external systems.
 * Implements MessagePublisherPort for Kafka, gRPC, and HTTP.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessagePublisherAdapter implements MessagePublisherPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RestTemplate restTemplate;

    @Override
    public void publishToKafka(String topic, IntegrationMessage message) {
        log.debug("Publishing message {} to Kafka topic: {}", message.getMessageId(), topic);
        try {
            String payload = convertToJson(message);
            kafkaTemplate.send(topic, message.getMessageId(), payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish to Kafka: {}", ex.getMessage());
                        } else {
                            log.debug("Published to Kafka: {}", result.getRecordMetadata().topic());
                        }
                    });
        } catch (Exception e) {
            log.error("Error publishing to Kafka", e);
            throw new MessagePublishException("Kafka publish failed", e);
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

    private String convertToJson(IntegrationMessage message) {
        // Simple JSON conversion - in production use Jackson
        return String.format(
            "{\"messageId\":\"%s\",\"type\":\"%s\",\"status\":\"%s\",\"payload\":\"%s\"}",
            message.getMessageId(),
            message.getType(),
            message.getStatus(),
            escapeJson(message.getTransformedPayload() != null ? message.getTransformedPayload() : message.getRawPayload())
        );
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
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
