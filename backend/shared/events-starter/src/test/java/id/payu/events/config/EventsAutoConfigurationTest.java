package id.payu.events.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EventsAutoConfiguration")
class EventsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EventsAutoConfiguration.class));

    @Nested
    @DisplayName("Default configuration")
    class DefaultConfig {

        @Test
        @DisplayName("should auto-configure when payu.events.enabled is not set (matchIfMissing)")
        void enabledByDefault() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(EventsAutoConfiguration.CloudEventsInitializer.class);
                assertThat(context).hasBean("cloudEventsObjectMapper");
            });
        }

        @Test
        @DisplayName("should create cloudEventsObjectMapper bean")
        void objectMapperBean() {
            contextRunner.run(context -> {
                assertThat(context).hasBean("cloudEventsObjectMapper");
                var mapper = context.getBean("cloudEventsObjectMapper", com.fasterxml.jackson.databind.ObjectMapper.class);
                assertThat(mapper).isNotNull();
            });
        }
    }

    @Nested
    @DisplayName("Disabled configuration")
    class DisabledConfig {

        @Test
        @DisplayName("should not auto-configure when payu.events.enabled=false")
        void disabledExplicitly() {
            contextRunner
                    .withPropertyValues("payu.events.enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(EventsAutoConfiguration.CloudEventsInitializer.class);
                    });
        }
    }

    @Nested
    @DisplayName("EventsProperties")
    class PropertiesTest {

        @Test
        @DisplayName("should bind properties from application config")
        void bindProperties() {
            contextRunner
                    .withPropertyValues(
                            "payu.events.enabled=true",
                            "payu.events.kafka.enabled=true",
                            "payu.events.kafka.default-topic=payu.custom.events",
                            "payu.events.kafka.bootstrap-servers=kafka:29092"
                    )
                    .run(context -> {
                        var props = context.getBean(EventsAutoConfiguration.EventsProperties.class);
                        assertThat(props.isEnabled()).isTrue();
                        assertThat(props.getKafka().isEnabled()).isTrue();
                        assertThat(props.getKafka().getDefaultTopic()).isEqualTo("payu.custom.events");
                        assertThat(props.getKafka().getBootstrapServers()).isEqualTo("kafka:29092");
                    });
        }

        @Test
        @DisplayName("should have sensible defaults")
        void defaults() {
            var props = new EventsAutoConfiguration.EventsProperties();
            assertThat(props.isEnabled()).isTrue();
            assertThat(props.getKafka().isEnabled()).isFalse();
            assertThat(props.getKafka().getDefaultTopic()).isEqualTo("payu.events");
            assertThat(props.getKafka().getBootstrapServers()).isEqualTo("localhost:9092");
        }
    }

    @Nested
    @DisplayName("Kafka Error Handler configuration")
    class KafkaErrorHandlerTest {

        @Test
        @DisplayName("should configure CommonErrorHandler and route to correct DLQ topic")
        void kafkaErrorHandlerConfigured() {
            org.springframework.kafka.core.KafkaTemplate<Object, Object> mockKafkaTemplate = 
                    org.mockito.Mockito.mock(org.springframework.kafka.core.KafkaTemplate.class);
            
            java.util.concurrent.CompletableFuture<org.springframework.kafka.support.SendResult<Object, Object>> mockCompletableFuture = 
                    java.util.concurrent.CompletableFuture.completedFuture(null);
            
            org.mockito.Mockito.when(mockKafkaTemplate.send(org.mockito.Mockito.any(org.apache.kafka.clients.producer.ProducerRecord.class)))
                    .thenReturn(mockCompletableFuture);

            contextRunner
                    .withBean(org.springframework.kafka.core.KafkaTemplate.class, () -> mockKafkaTemplate)
                    .run(context -> {
                        assertThat(context).hasSingleBean(org.springframework.kafka.listener.CommonErrorHandler.class);
                        var errorHandler = context.getBean(org.springframework.kafka.listener.CommonErrorHandler.class);
                        assertThat(errorHandler).isInstanceOf(org.springframework.kafka.listener.DefaultErrorHandler.class);
                        
                        var defaultErrorHandler = (org.springframework.kafka.listener.DefaultErrorHandler) errorHandler;
                        Object failureTracker = org.springframework.test.util.ReflectionTestUtils.getField(defaultErrorHandler, "failureTracker");
                        var recoverer = (org.springframework.kafka.listener.DeadLetterPublishingRecoverer) 
                                org.springframework.test.util.ReflectionTestUtils.getField(failureTracker, "recoverer");
                        
                        org.apache.kafka.clients.consumer.ConsumerRecord<String, String> record = 
                                new org.apache.kafka.clients.consumer.ConsumerRecord<>("payu.wallet.balance-changed.v1", 0, 123L, "key", "payload");
                        
                        recoverer.accept(record, new RuntimeException("Test exception"));
                        
                        org.mockito.ArgumentCaptor<org.apache.kafka.clients.producer.ProducerRecord> recordCaptor = 
                                org.mockito.ArgumentCaptor.forClass(org.apache.kafka.clients.producer.ProducerRecord.class);
                        
                        org.mockito.Mockito.verify(mockKafkaTemplate).send(recordCaptor.capture());
                        
                        org.apache.kafka.clients.producer.ProducerRecord<?, ?> capturedRecord = recordCaptor.getValue();
                        assertThat(capturedRecord.topic()).isEqualTo("payu.wallet.balance-changed.v1.dlq");
                    });
        }
    }
}
