package id.payu.saga.config;

import id.payu.saga.repository.SagaRepository;
import id.payu.saga.service.SagaMonitorService;
import id.payu.saga.service.SagaRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auto-configuration for saga pattern support.
 * Automatically configures saga infrastructure when the starter is added to the classpath.
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(SagaProperties.class)
@ConditionalOnProperty(prefix = "payu.saga", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(SagaRepository.class)
@ComponentScan(basePackages = "id.payu.saga")
@EntityScan(basePackages = "id.payu.saga.entity")
@EnableJpaRepositories(basePackages = "id.payu.saga.repository")
public class SagaAutoConfiguration {

    private final SagaProperties properties;

    public SagaAutoConfiguration(SagaProperties properties) {
        this.properties = properties;
        log.info("Saga pattern support auto-configured with properties: {}", properties);
    }

    /**
     * Saga monitoring service bean.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "payu.saga", name = "monitoring-enabled", havingValue = "true", matchIfMissing = true)
    public SagaMonitorService sagaMonitorService(SagaRepository sagaRepository) {
        return new SagaMonitorService(sagaRepository, properties);
    }

    /**
     * Saga recovery service bean.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "payu.saga", name = "compensation-enabled", havingValue = "true", matchIfMissing = true)
    public SagaRecoveryService sagaRecoveryService(SagaRepository sagaRepository) {
        return new SagaRecoveryService(sagaRepository, properties);
    }

    /**
     * Saga event publisher bean (if Kafka is available).
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(KafkaTemplate.class)
    @ConditionalOnProperty(prefix = "payu.saga", name = "events-enabled", havingValue = "true", matchIfMissing = true)
    public SagaEventPublisher sagaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        return new SagaEventPublisher(kafkaTemplate, properties);
    }

    /**
     * Saga cleanup service bean.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "payu.saga", name = "cleanup-enabled", havingValue = "true", matchIfMissing = true)
    public SagaCleanupService sagaCleanupService(SagaRepository sagaRepository) {
        return new SagaCleanupService(sagaRepository, properties);
    }

    /**
     * Saga configuration validator.
     */
    @Bean
    public SagaConfigurationValidator sagaConfigurationValidator() {
        return new SagaConfigurationValidator(properties);
    }

    /**
     * Inner class for event publishing.
     */
    @RequiredArgsConstructor
    public static class SagaEventPublisher {
        private final KafkaTemplate<String, Object> kafkaTemplate;
        private final SagaProperties properties;

        public void publishSagaEvent(String sagaId, String eventType, Object payload) {
            if (properties.isEventsEnabled()) {
                kafkaTemplate.send(properties.getEventTopic(), sagaId, payload);
            }
        }
    }

    /**
     * Inner class for cleanup service.
     */
    @RequiredArgsConstructor
    public static class SagaCleanupService {
        private final SagaRepository sagaRepository;
        private final SagaProperties properties;

        @org.springframework.scheduling.annotation.Scheduled(cron = "${payu.saga.cleanup-schedule:0 0 2 * * ?}")
        public void cleanupOldSagas() {
            if (properties.isCleanupEnabled()) {
                java.time.Instant threshold = java.time.Instant.now().minus(properties.getRetentionPeriod());
                int deleted = sagaRepository.deleteOldCompletedSagas(threshold);
                log.info("Cleaned up {} old completed saga instances", deleted);
            }
        }
    }

    /**
     * Inner class for configuration validation.
     */
    @RequiredArgsConstructor
    public static class SagaConfigurationValidator {
        private final SagaProperties properties;

        @jakarta.annotation.PostConstruct
        public void validate() {
            if (properties.getDefaultMaxRetries() < 0) {
                throw new IllegalArgumentException("Saga defaultMaxRetries must be >= 0");
            }
            if (properties.getDefaultTimeout().isNegative() || properties.getDefaultTimeout().isZero()) {
                throw new IllegalArgumentException("Saga defaultTimeout must be positive");
            }
            log.info("Saga configuration validated successfully");
        }
    }
}
