package id.payu.events.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.cloudevents.core.message.MessageReader;
import io.cloudevents.core.message.MessageWriter;
import io.cloudevents.jackson.JsonFormat;
import io.cloudevents.jackson.PojoCloudEventDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Spring Boot Auto Configuration for PayU CloudEvents support.
 * Automatically configures CloudEvents infrastructure when the starter is added.
 */
@Slf4j
@AutoConfiguration(after = org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration.class)
@EnableConfigurationProperties(EventsAutoConfiguration.EventsProperties.class)
@ComponentScan(basePackages = "id.payu.events")
@ConditionalOnProperty(
    prefix = "payu.events",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class EventsAutoConfiguration {

    /**
     * Configuration properties for CloudEvents.
     */
    @lombok.Data
    @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "payu.events")
    public static class EventsProperties {
        private boolean enabled = true;
        private KafkaProperties kafka = new KafkaProperties();

        @lombok.Data
        public static class KafkaProperties {
            private boolean enabled = false;
            private String defaultTopic = "payu.events";
            private String bootstrapServers = "localhost:9092";
        }
    }

    /**
     * Configures ObjectMapper for CloudEvents serialization.
     * Registers JavaTimeModule for proper OffsetDateTime handling.
     *
     * @return Configured ObjectMapper
     */
    @Bean
    @ConditionalOnMissingBean(name = "cloudEventsObjectMapper")
    public ObjectMapper cloudEventsObjectMapper() {
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);

        log.info("CloudEvents ObjectMapper configured");
        return mapper;
    }

    /**
     * Configures CloudEvents JSON format.
     *
     * @return JsonFormat instance
     */
    @Bean
    @ConditionalOnMissingBean
    public JsonFormat cloudEventsJsonFormat() {
        return new JsonFormat();
    }

    /**
     * Configuration for Kafka-based CloudEvent publishing and consuming.
     * Only active when Kafka is on the classpath.
     */
    @Configuration
    @ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(org.springframework.kafka.core.KafkaTemplate.class)
    static class KafkaCloudEventsConfiguration {

        public KafkaCloudEventsConfiguration() {
            log.info("Kafka CloudEvents support enabled");
        }

        @Bean
        @ConditionalOnMissingBean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(org.springframework.kafka.core.KafkaTemplate.class)
        public org.springframework.kafka.listener.CommonErrorHandler kafkaErrorHandler(
                org.springframework.kafka.core.KafkaTemplate<?, ?> kafkaTemplate) {
            
            log.info("Configuring DefaultErrorHandler with DeadLetterPublishingRecoverer (DLQ)");
            
            // Send failed records to <topic>.dlq
            org.springframework.kafka.listener.DeadLetterPublishingRecoverer recoverer =
                    new org.springframework.kafka.listener.DeadLetterPublishingRecoverer(kafkaTemplate,
                            (dbRecord, ex) -> {
                                String dlqTopic = dbRecord.topic() + ".dlq";
                                log.warn("Forwarding failed record to DLQ: topic={}, partition={}, offset={}, exception={}",
                                        dlqTopic, dbRecord.partition(), dbRecord.offset(), ex.getMessage());
                                return new org.apache.kafka.common.TopicPartition(dlqTopic, -1);
                            });
            
            // 3 retries (4 total attempts), 1s fixed delay
            org.springframework.util.backoff.FixedBackOff backOff =
                    new org.springframework.util.backoff.FixedBackOff(1000L, 3L);
            
            org.springframework.kafka.listener.DefaultErrorHandler errorHandler =
                    new org.springframework.kafka.listener.DefaultErrorHandler(recoverer, backOff);
            
            errorHandler.setCommitRecovered(true);
            return errorHandler;
        }
    }

    /**
     * Initializes the CloudEvents starter.
     */
    @Bean
    public CloudEventsInitializer cloudEventsInitializer() {
        return new CloudEventsInitializer();
    }

    /**
     * Initializer component that logs startup information.
     */
    @Slf4j
    public static class CloudEventsInitializer {
        @jakarta.annotation.PostConstruct
        public void init() {
            log.info("PayU CloudEvents Starter initialized - specVersion: 1.0.2");
        }
    }
}
