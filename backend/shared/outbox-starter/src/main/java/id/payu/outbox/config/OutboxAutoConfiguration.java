package id.payu.outbox.config;

import id.payu.outbox.publisher.OutboxPublisher;
import id.payu.outbox.repository.OutboxRepository;
import id.payu.outbox.service.OutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.HashMap;
import java.util.Map;

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
@AutoConfiguration(before = KafkaAutoConfiguration.class)
@ConditionalOnClass({OutboxRepository.class, KafkaTemplate.class})
@ConditionalOnProperty(prefix = "payu.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OutboxProperties.class)
@EnableScheduling
@EntityScan(basePackages = "id.payu.outbox.entity")
@EnableJpaRepositories(basePackages = "id.payu.outbox.repository")
@ComponentScan(basePackages = "id.payu.outbox")
public class OutboxAutoConfiguration {

    /**
     * Outbox producer must default to durable delivery: acks=all,
     * idempotence on, bounded retries (ARCH-PROD-001). Idempotence makes
     * retries safe against duplicates; acks=all survives broker failover.
     * Registered before {@link KafkaAutoConfiguration} so it wins for
     * services that do not define their own {@link ProducerFactory}.
     */
    @Bean
    @ConditionalOnMissingBean(ProducerFactory.class)
    public ProducerFactory<String, String> outboxProducerFactory(
            @org.springframework.beans.factory.annotation.Value("${spring.kafka.bootstrap-servers:localhost:9092}")
            String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.RETRIES_CONFIG, 5);
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * Creates the OutboxPublisher bean if not already defined.
     *
     * @param outboxRepository the outbox repository
     * @param kafkaTemplate the Kafka template for publishing
     * @param meterRegistry the meter registry for metrics
     * @param transactionManager the platform transaction manager for mark-before-send pattern
     * @return the configured OutboxPublisher
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(KafkaTemplate.class)
    @ConditionalOnProperty(prefix = "payu.outbox.publisher", name = "enabled", havingValue = "true", matchIfMissing = true)
    public OutboxPublisher outboxPublisher(OutboxRepository outboxRepository,
                                           KafkaTemplate<String, String> kafkaTemplate,
                                           MeterRegistry meterRegistry,
                                           PlatformTransactionManager transactionManager) {
        log.info("Initializing OutboxPublisher with auto-configuration");
        return new OutboxPublisher(outboxRepository, kafkaTemplate, meterRegistry, transactionManager);
    }

    /**
     * Creates the OutboxService bean if not already defined.
     *
     * @param outboxRepository the outbox repository
     * @return the configured OutboxService
     */
    @Bean
    @ConditionalOnMissingBean
    public OutboxService outboxService(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        log.info("Initializing OutboxService with auto-configuration");
        return new OutboxService(outboxRepository, objectMapper);
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
