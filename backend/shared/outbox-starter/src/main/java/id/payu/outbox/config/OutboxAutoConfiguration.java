package id.payu.outbox.config;

import id.payu.outbox.publisher.OutboxPublisher;
import id.payu.outbox.repository.OutboxRepository;
import id.payu.outbox.service.OutboxService;
import io.micrometer.core.instrument.MeterRegistry;
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
 * Auto-configuration for the Transactional Outbox Pattern starter.
 * <p>
 * This configuration class automatically sets up all necessary components
 * for the outbox pattern when the starter is added to a service's classpath.
 * <p>
 * Features:
 * <ul>
 *   <li>Automatic entity scanning for {@link id.payu.outbox.entity.OutboxEvent}</li>
 *   <li>Repository configuration for {@link OutboxRepository}</li>
 *   <li>Publisher scheduling and configuration</li>
 *   <li>Outbox service for event creation</li>
 *   <li>Conditional activation via {@code payu.outbox.enabled} property</li>
 * </ul>
 *
 * @author PayU Digital Banking Platform
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({OutboxRepository.class, KafkaTemplate.class})
@ConditionalOnProperty(prefix = "payu.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OutboxProperties.class)
@EnableScheduling
@EntityScan(basePackages = "id.payu.outbox.entity")
@EnableJpaRepositories(basePackages = "id.payu.outbox.repository")
@ComponentScan(basePackages = "id.payu.outbox")
public class OutboxAutoConfiguration {

    /**
     * Creates the OutboxPublisher bean if not already defined.
     *
     * @param outboxRepository the outbox repository
     * @param kafkaTemplate the Kafka template for publishing
     * @param meterRegistry the meter registry for metrics
     * @return the configured OutboxPublisher
     */
    @Bean
    @ConditionalOnMissingBean
    public OutboxPublisher outboxPublisher(OutboxRepository outboxRepository,
                                           KafkaTemplate<String, String> kafkaTemplate,
                                           MeterRegistry meterRegistry) {
        log.info("Initializing OutboxPublisher with auto-configuration");
        OutboxPublisher publisher = new OutboxPublisher(outboxRepository, kafkaTemplate, meterRegistry);
        publisher.init();
        return publisher;
    }

    /**
     * Creates the OutboxService bean if not already defined.
     *
     * @param outboxRepository the outbox repository
     * @return the configured OutboxService
     */
    @Bean
    @ConditionalOnMissingBean
    public OutboxService outboxService(OutboxRepository outboxRepository) {
        log.info("Initializing OutboxService with auto-configuration");
        return new OutboxService(outboxRepository);
    }

    /**
     * Configuration properties for the outbox starter.
     */
    @Configuration
    @EnableConfigurationProperties
    public static class OutboxPropertiesConfiguration {
        // Properties are loaded via @EnableConfigurationProperties on the outer class
    }
}
