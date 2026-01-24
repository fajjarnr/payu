package id.payu.abtesting.infrastructure.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.abtesting.domain.entity.Experiment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Kafka producer for experiment events
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ExperimentEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String EXPERIMENT_TOPIC = "ab-testing.experiments";
    private static final String ASSIGNMENT_TOPIC = "ab-testing.assignments";
    private static final String CONVERSION_TOPIC = "ab-testing.conversions";

    /**
     * Publish experiment created event
     */
    public void publishExperimentCreated(Experiment experiment) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "eventType", "experiment.created",
                    "experimentId", experiment.getId().toString(),
                    "key", experiment.getKey(),
                    "name", experiment.getName(),
                    "status", experiment.getStatus().name(),
                    "timestamp", System.currentTimeMillis()
            ));
            kafkaTemplate.send(EXPERIMENT_TOPIC, experiment.getId().toString(), payload);
            log.debug("Published experiment created event: {}", experiment.getId());
        } catch (Exception e) {
            log.error("Failed to publish experiment created event", e);
        }
    }

    /**
     * Publish experiment updated event
     */
    public void publishExperimentUpdated(Experiment experiment) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "eventType", "experiment.updated",
                    "experimentId", experiment.getId().toString(),
                    "key", experiment.getKey(),
                    "status", experiment.getStatus().name(),
                    "timestamp", System.currentTimeMillis()
            ));
            kafkaTemplate.send(EXPERIMENT_TOPIC, experiment.getId().toString(), payload);
            log.debug("Published experiment updated event: {}", experiment.getId());
        } catch (Exception e) {
            log.error("Failed to publish experiment updated event", e);
        }
    }

    /**
     * Publish experiment deleted event
     */
    public void publishExperimentDeleted(UUID experimentId) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "eventType", "experiment.deleted",
                    "experimentId", experimentId.toString(),
                    "timestamp", System.currentTimeMillis()
            ));
            kafkaTemplate.send(EXPERIMENT_TOPIC, experimentId.toString(), payload);
            log.debug("Published experiment deleted event: {}", experimentId);
        } catch (Exception e) {
            log.error("Failed to publish experiment deleted event", e);
        }
    }

    /**
     * Publish variant assigned event
     */
    public void publishVariantAssigned(UUID experimentId, UUID userId, String variant) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "eventType", "variant.assigned",
                    "experimentId", experimentId.toString(),
                    "userId", userId.toString(),
                    "variant", variant,
                    "timestamp", System.currentTimeMillis()
            ));
            kafkaTemplate.send(ASSIGNMENT_TOPIC, userId.toString(), payload);
            log.debug("Published variant assigned event: user={}, variant={}", userId, variant);
        } catch (Exception e) {
            log.error("Failed to publish variant assigned event", e);
        }
    }

    /**
     * Publish conversion tracked event
     */
    public void publishConversionTracked(UUID experimentId, UUID userId, String variant, String eventType) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "eventType", "conversion.tracked",
                    "experimentId", experimentId.toString(),
                    "userId", userId.toString(),
                    "variant", variant,
                    "conversionType", eventType,
                    "timestamp", System.currentTimeMillis()
            ));
            kafkaTemplate.send(CONVERSION_TOPIC, userId.toString(), payload);
            log.debug("Published conversion tracked event: user={}, variant={}, type={}", userId, variant, eventType);
        } catch (Exception e) {
            log.error("Failed to publish conversion tracked event", e);
        }
    }

    /**
     * Publish status changed event
     */
    public void publishStatusChanged(Experiment experiment) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "eventType", "status.changed",
                    "experimentId", experiment.getId().toString(),
                    "key", experiment.getKey(),
                    "status", experiment.getStatus().name(),
                    "timestamp", System.currentTimeMillis()
            ));
            kafkaTemplate.send(EXPERIMENT_TOPIC, experiment.getId().toString(), payload);
            log.debug("Published status changed event: {} -> {}", experiment.getKey(), experiment.getStatus());
        } catch (Exception e) {
            log.error("Failed to publish status changed event", e);
        }
    }
}
