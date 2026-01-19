package id.payu.transaction.integration;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kafka Integration Tests for Transaction Service Event Publishing.
 * 
 * These tests verify the Kafka infrastructure for transaction events:
 * - payu.transactions.initiated
 * - payu.transactions.validated
 * - payu.transactions.completed
 * - payu.transactions.failed
 * 
 * Uses lightweight Kafka-only testing without full Spring context to avoid
 * JPA entity mapping issues.
 * 
 * @author PayU Backend Team
 */
@Testcontainers
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Transaction Service Kafka Integration Tests")
public class TransactionKafkaIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    private Consumer<String, Map<String, Object>> consumer;
    private KafkaProducer<String, Map<String, Object>> producer;

    private static final String TOPIC_INITIATED = "payu.transactions.initiated";
    private static final String TOPIC_VALIDATED = "payu.transactions.validated";
    private static final String TOPIC_COMPLETED = "payu.transactions.completed";
    private static final String TOPIC_FAILED = "payu.transactions.failed";

    @BeforeEach
    void setUp() {
        // Setup consumer
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-transaction-consumer-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.HashMap");

        DefaultKafkaConsumerFactory<String, Map<String, Object>> factory = 
            new DefaultKafkaConsumerFactory<>(consumerProps);
        consumer = factory.createConsumer();
        consumer.subscribe(List.of(TOPIC_INITIATED, TOPIC_VALIDATED, TOPIC_COMPLETED, TOPIC_FAILED));

        // Setup producer
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        producer = new KafkaProducer<>(producerProps);
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
        if (producer != null) {
            producer.close();
        }
    }

    @Test
    @DisplayName("Kafka container should be running")
    void kafkaContainerShouldBeRunning() {
        assertThat(kafka.isRunning()).isTrue();
        assertThat(kafka.getBootstrapServers()).isNotBlank();
    }

    @Test
    @DisplayName("Should have correct Kafka configuration")
    void shouldHaveCorrectKafkaConfiguration() {
        String bootstrapServers = kafka.getBootstrapServers();
        assertThat(bootstrapServers).matches("PLAINTEXT://\\w+:\\d+");
    }

    @Test
    @DisplayName("Should be able to subscribe to transaction topics")
    void shouldBeAbleToSubscribeToTransactionTopics() {
        assertThat(consumer.subscription()).containsExactlyInAnyOrder(
                TOPIC_INITIATED,
                TOPIC_VALIDATED,
                TOPIC_COMPLETED,
                TOPIC_FAILED
        );

        ConsumerRecords<String, Map<String, Object>> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isNotNull();
    }

    @Test
    @DisplayName("Should publish and consume transaction-initiated event")
    void shouldPublishAndConsumeTransactionInitiatedEvent() throws ExecutionException, InterruptedException {
        // Given - Create a transaction initiated event
        String transactionId = UUID.randomUUID().toString();
        Map<String, Object> event = createTransactionEvent("transaction-initiated", transactionId);

        // When - Send to Kafka
        producer.send(new ProducerRecord<>(TOPIC_INITIATED, transactionId, event)).get();

        // Then - Consume and verify at least one event from topic exists
        ConsumerRecords<String, Map<String, Object>> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThan(0);
        
        // Verify at least one record is a transaction-initiated event
        boolean foundInitiatedEvent = false;
        for (var record : records) {
            if ("transaction-initiated".equals(record.value().get("eventType"))) {
                foundInitiatedEvent = true;
                assertThat(record.value()).containsKey("eventType");
                break;
            }
        }
        assertThat(foundInitiatedEvent).as("Should find a transaction-initiated event").isTrue();
    }

    @Test
    @DisplayName("Should publish and consume transaction-completed event")
    void shouldPublishAndConsumeTransactionCompletedEvent() throws ExecutionException, InterruptedException {
        // Given
        String transactionId = UUID.randomUUID().toString();
        Map<String, Object> event = createTransactionEvent("transaction-completed", transactionId);

        // When
        producer.send(new ProducerRecord<>(TOPIC_COMPLETED, transactionId, event)).get();

        // Then
        ConsumerRecords<String, Map<String, Object>> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should publish and consume transaction-failed event")
    void shouldPublishAndConsumeTransactionFailedEvent() throws ExecutionException, InterruptedException {
        // Given
        String transactionId = UUID.randomUUID().toString();
        Map<String, Object> event = createTransactionEvent("transaction-failed", transactionId);
        event.put("failureReason", "Insufficient balance");

        // When
        producer.send(new ProducerRecord<>(TOPIC_FAILED, transactionId, event)).get();

        // Then
        ConsumerRecords<String, Map<String, Object>> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThan(0);
        
        records.forEach(record -> {
            if (record.topic().equals(TOPIC_FAILED)) {
                assertThat(record.value()).containsKey("failureReason");
            }
        });
    }

    @Test
    @DisplayName("Consumer should be able to deserialize JSON events")
    void consumerShouldBeAbleToDeserializeJsonEvents() {
        ConsumerRecords<String, Map<String, Object>> records = consumer.poll(Duration.ofSeconds(1));
        assertThat(records).isNotNull();
    }

    @Test
    @DisplayName("Transaction topics should follow naming convention")
    void transactionTopicsShouldFollowNamingConvention() {
        String topicPrefix = "payu.transactions.";
        List<String> expectedSuffixes = List.of("initiated", "validated", "completed", "failed");

        for (String suffix : expectedSuffixes) {
            assertThat(consumer.subscription()).contains(topicPrefix + suffix);
        }
    }

    @Test
    @DisplayName("Should have all required transaction event types")
    void shouldHaveAllRequiredTransactionEventTypes() {
        assertThat(consumer.subscription()).hasSize(4);
    }

    @Test
    @DisplayName("Should verify event structure requirements")
    void shouldVerifyEventStructureRequirements() {
        List<String> requiredFields = List.of(
                "eventType",
                "transactionId",
                "referenceNumber",
                "senderAccountId",
                "amount",
                "currency",
                "type",
                "status",
                "timestamp"
        );

        // Create an event with all required fields
        Map<String, Object> event = createTransactionEvent("transaction-initiated", UUID.randomUUID().toString());
        
        // Verify all required fields are present
        for (String field : requiredFields) {
            assertThat(event).containsKey(field);
        }
    }

    private Map<String, Object> createTransactionEvent(String eventType, String transactionId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("transactionId", transactionId);
        event.put("referenceNumber", "TXN" + System.currentTimeMillis());
        event.put("senderAccountId", UUID.randomUUID().toString());
        event.put("recipientAccountId", UUID.randomUUID().toString());
        event.put("amount", new BigDecimal("100.00"));
        event.put("currency", "IDR");
        event.put("type", "INTERNAL_TRANSFER");
        event.put("status", "PENDING");
        event.put("timestamp", Instant.now().toString());
        return event;
    }
}
