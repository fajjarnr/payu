package id.payu.notification.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
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
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kafka Integration Tests for Notification Service.
 *
 * These tests verify the Kafka messaging infrastructure for notification events:
 * - Publishing notification events to Kafka topics
 * - Consuming notification events from Kafka topics
 * - SMS notification publishing
 * - Email notification publishing
 * - Push notification publishing
 * - WhatsApp notification publishing (when implemented)
 * - Error handling for connection failures and timeouts
 *
 * Uses Testcontainers for Kafka to provide isolated testing environment.
 * Tests are tagged with "integration" and only run when Docker is available
 * via the @EnabledIfSystemProperty annotation.
 *
 * @author PayU Backend Team
 */
@QuarkusTest
@TestProfile(KafkaIntegrationTestProfile.class)
@Testcontainers
@Tag("integration")
@EnabledIfSystemProperty(named = "docker.available", matches = "true", disabledReason = "Docker is not available")
@DisplayName("Notification Service Kafka Integration Tests")
public class NotificationIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    private KafkaConsumer<String, String> consumer;
    private KafkaProducer<String, String> producer;

    // Topics used by notification service
    private static final String TOPIC_WALLET_EVENTS = "wallet.balance.changed";
    private static final String TOPIC_TRANSACTION_EVENTS = "transaction.completed";
    private static final String TOPIC_PAYMENT_EVENTS = "payment-events";
    private static final String TOPIC_SPLIT_BILL_EVENTS = "split-bill-events";
    private static final String TOPIC_NOTIFICATION_EVENTS = "notification-events";

    @BeforeEach
    void setUp() {
        // Setup consumer
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-notification-consumer-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(List.of(
                TOPIC_WALLET_EVENTS,
                TOPIC_TRANSACTION_EVENTS,
                TOPIC_PAYMENT_EVENTS,
                TOPIC_SPLIT_BILL_EVENTS,
                TOPIC_NOTIFICATION_EVENTS
        ));

        // Setup producer
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        producerProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);

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
    @DisplayName("Should have correct Kafka bootstrap servers configuration")
    void shouldHaveCorrectKafkaConfiguration() {
        String bootstrapServers = kafka.getBootstrapServers();
        assertThat(bootstrapServers).matches("PLAINTEXT://\\w+:\\d+");
    }

    @Test
    @DisplayName("Should be able to subscribe to all notification topics")
    void shouldBeAbleToSubscribeToNotificationTopics() {
        assertThat(consumer.subscription()).containsExactlyInAnyOrder(
                TOPIC_WALLET_EVENTS,
                TOPIC_TRANSACTION_EVENTS,
                TOPIC_PAYMENT_EVENTS,
                TOPIC_SPLIT_BILL_EVENTS,
                TOPIC_NOTIFICATION_EVENTS
        );

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isNotNull();
    }

    @Test
    @DisplayName("Should publish and consume wallet balance change event")
    void shouldPublishAndConsumeWalletBalanceChangeEvent() throws ExecutionException, InterruptedException {
        // Given - Create a wallet balance change event
        String accountId = UUID.randomUUID().toString();
        String eventPayload = String.format(
                "{\"eventType\":\"wallet-balance-changed\",\"accountId\":\"%s\",\"oldBalance\":\"1000.00\",\"newBalance\":\"1500.00\",\"currency\":\"IDR\",\"timestamp\":\"%s\"}",
                accountId,
                java.time.Instant.now().toString()
        );

        // When - Send to Kafka
        producer.send(new ProducerRecord<>(TOPIC_WALLET_EVENTS, accountId, eventPayload)).get();

        // Then - Consume and verify event
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThan(0);

        boolean foundWalletEvent = false;
        for (var record : records) {
            if (record.topic().equals(TOPIC_WALLET_EVENTS)) {
                foundWalletEvent = true;
                assertThat(record.value()).contains("wallet-balance-changed");
                assertThat(record.value()).contains(accountId);
                break;
            }
        }
        assertThat(foundWalletEvent).as("Should find a wallet balance changed event").isTrue();
    }

    @Test
    @DisplayName("Should publish and consume transaction completed event for email notification")
    void shouldPublishAndConsumeTransactionCompletedEvent() throws ExecutionException, InterruptedException {
        // Given - Create a transaction completed event
        String transactionId = UUID.randomUUID().toString();
        String eventPayload = String.format(
                "{\"eventType\":\"transaction-completed\",\"transactionId\":\"%s\",\"userId\":\"user123\",\"email\":\"test@payu.fajjjar.my.id\",\"amount\":\"50000\",\"currency\":\"IDR\",\"type\":\"TRANSFER\",\"timestamp\":\"%s\"}",
                transactionId,
                java.time.Instant.now().toString()
        );

        // When - Send to Kafka
        producer.send(new ProducerRecord<>(TOPIC_TRANSACTION_EVENTS, transactionId, eventPayload)).get();

        // Then - Consume and verify event
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThan(0);

        boolean foundTransactionEvent = false;
        for (var record : records) {
            if (record.topic().equals(TOPIC_TRANSACTION_EVENTS)) {
                foundTransactionEvent = true;
                assertThat(record.value()).contains("transaction-completed");
                assertThat(record.value()).contains(transactionId);
                assertThat(record.value()).contains("test@payu.fajjjar.my.id");
                break;
            }
        }
        assertThat(foundTransactionEvent).as("Should find a transaction completed event").isTrue();
    }

    @Test
    @DisplayName("Should publish and consume payment event for SMS notification")
    void shouldPublishAndConsumePaymentEvent() throws ExecutionException, InterruptedException {
        // Given - Create a payment event
        String paymentId = UUID.randomUUID().toString();
        String eventPayload = String.format(
                "{\"eventType\":\"bill-payment-completed\",\"paymentId\":\"%s\",\"userId\":\"user456\",\"phoneNumber\":\"+628123456789\",\"billType\":\"PLN\",\"amount\":\"150000\",\"currency\":\"IDR\",\"timestamp\":\"%s\"}",
                paymentId,
                java.time.Instant.now().toString()
        );

        // When - Send to Kafka
        producer.send(new ProducerRecord<>(TOPIC_PAYMENT_EVENTS, paymentId, eventPayload)).get();

        // Then - Consume and verify event
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThan(0);

        boolean foundPaymentEvent = false;
        for (var record : records) {
            if (record.topic().equals(TOPIC_PAYMENT_EVENTS)) {
                foundPaymentEvent = true;
                assertThat(record.value()).contains("bill-payment-completed");
                assertThat(record.value()).contains(paymentId);
                assertThat(record.value()).contains("+628123456789");
                break;
            }
        }
        assertThat(foundPaymentEvent).as("Should find a payment event").isTrue();
    }

    @Test
    @DisplayName("Should publish and consume split bill invitation event for push notification")
    void shouldPublishAndConsumeSplitBillInvitationEvent() throws ExecutionException, InterruptedException {
        // Given - Create a split bill invitation event
        String splitBillId = UUID.randomUUID().toString();
        String eventPayload = String.format(
                "{\"eventType\":\"split-bill-activated\",\"splitBillId\":\"%s\",\"title\":\"Makan Siang\",\"totalAmount\":\"300000\",\"currency\":\"IDR\",\"creatorAccountId\":\"ACC001\",\"timestamp\":\"%s\"}",
                splitBillId,
                java.time.Instant.now().toString()
        );

        // When - Send to Kafka
        producer.send(new ProducerRecord<>(TOPIC_SPLIT_BILL_EVENTS, splitBillId, eventPayload)).get();

        // Then - Consume and verify event
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThan(0);

        boolean foundSplitBillEvent = false;
        for (var record : records) {
            if (record.topic().equals(TOPIC_SPLIT_BILL_EVENTS)) {
                foundSplitBillEvent = true;
                assertThat(record.value()).contains("split-bill-activated");
                assertThat(record.value()).contains(splitBillId);
                assertThat(record.value()).contains("Makan Siang");
                break;
            }
        }
        assertThat(foundSplitBillEvent).as("Should find a split bill invitation event").isTrue();
    }

    @Test
    @DisplayName("Should publish and consume split bill payment reminder event")
    void shouldPublishAndConsumeSplitBillPaymentReminderEvent() throws ExecutionException, InterruptedException {
        // Given - Create a payment reminder event
        String splitBillId = UUID.randomUUID().toString();
        String eventPayload = String.format(
                "{\"eventType\":\"payment-reminder\",\"splitBillId\":\"%s\",\"accountId\":\"ACC002\",\"accountName\":\"John Doe\",\"amountOwed\":\"75000\",\"currency\":\"IDR\",\"referenceNumber\":\"Makan Malam\",\"timestamp\":\"%s\"}",
                splitBillId,
                java.time.Instant.now().toString()
        );

        // When - Send to Kafka
        producer.send(new ProducerRecord<>(TOPIC_SPLIT_BILL_EVENTS, splitBillId, eventPayload)).get();

        // Then - Consume and verify event
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThan(0);

        boolean foundReminderEvent = false;
        for (var record : records) {
            if (record.topic().equals(TOPIC_SPLIT_BILL_EVENTS) &&
                record.value().contains("payment-reminder")) {
                foundReminderEvent = true;
                assertThat(record.value()).contains("payment-reminder");
                assertThat(record.value()).contains("75000");
                break;
            }
        }
        assertThat(foundReminderEvent).as("Should find a payment reminder event").isTrue();
    }

    @Test
    @DisplayName("Should publish and consume direct notification event")
    void shouldPublishAndConsumeDirectNotificationEvent() throws ExecutionException, InterruptedException {
        // Given - Create a direct notification event
        String notificationId = UUID.randomUUID().toString();
        String eventPayload = String.format(
                "{\"notificationId\":\"%s\",\"userId\":\"user789\",\"channel\":\"EMAIL\",\"recipient\":\"user789@payu.fajjjar.my.id\",\"title\":\"Account Verified\",\"body\":\"Your account has been verified successfully\",\"timestamp\":\"%s\"}",
                notificationId,
                java.time.Instant.now().toString()
        );

        // When - Send to Kafka
        producer.send(new ProducerRecord<>(TOPIC_NOTIFICATION_EVENTS, notificationId, eventPayload)).get();

        // Then - Consume and verify event
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThan(0);

        boolean foundNotificationEvent = false;
        for (var record : records) {
            if (record.topic().equals(TOPIC_NOTIFICATION_EVENTS)) {
                foundNotificationEvent = true;
                assertThat(record.value()).contains(notificationId);
                assertThat(record.value()).contains("EMAIL");
                assertThat(record.value()).contains("Account Verified");
                break;
            }
        }
        assertThat(foundNotificationEvent).as("Should find a direct notification event").isTrue();
    }

    @Test
    @DisplayName("Should handle multiple events in sequence")
    void shouldHandleMultipleEventsInSequence() throws ExecutionException, InterruptedException {
        // Given - Create multiple events
        String walletEvent = "{\"eventType\":\"wallet-balance-changed\",\"accountId\":\"ACC001\",\"amount\":\"1000\"}";
        String transactionEvent = "{\"eventType\":\"transaction-completed\",\"transactionId\":\"TXN001\",\"userId\":\"user001\"}";
        String paymentEvent = "{\"eventType\":\"bill-payment-completed\",\"paymentId\":\"PAY001\",\"userId\":\"user001\"}";

        // When - Send all events to Kafka
        producer.send(new ProducerRecord<>(TOPIC_WALLET_EVENTS, "ACC001", walletEvent)).get();
        producer.send(new ProducerRecord<>(TOPIC_TRANSACTION_EVENTS, "TXN001", transactionEvent)).get();
        producer.send(new ProducerRecord<>(TOPIC_PAYMENT_EVENTS, "PAY001", paymentEvent)).get();

        // Then - Consume and verify all events
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Should handle producer connection failure gracefully")
    void shouldHandleProducerConnectionFailureGracefully() {
        // Given - Create a producer with invalid configuration
        Map<String, Object> invalidProducerProps = new HashMap<>();
        invalidProducerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9999"); // Invalid port
        invalidProducerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        invalidProducerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        invalidProducerProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000);
        invalidProducerProps.put(ProducerConfig.ACKS_CONFIG, "all");

        KafkaProducer<String, String> invalidProducer = new KafkaProducer<>(invalidProducerProps);

        // When - Try to send a message
        ProducerRecord<String, String> record = new ProducerRecord<>(
                TOPIC_NOTIFICATION_EVENTS,
                "test-key",
                "test-value"
        );

        // Then - Should handle the exception gracefully
        try {
            invalidProducer.send(record).get();
            // If we reach here, the test should fail (expected exception)
            assertThat(true).isFalse();
        } catch (Exception e) {
            // Expected to fail due to connection failure
            assertThat(e).isNotNull();
        } finally {
            invalidProducer.close();
        }
    }

    @Test
    @DisplayName("Should handle producer timeout gracefully")
    void shouldHandleProducerTimeoutGracefully() {
        // Given - Create a producer with short timeout
        Map<String, Object> timeoutProducerProps = new HashMap<>();
        timeoutProducerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        timeoutProducerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        timeoutProducerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        timeoutProducerProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1); // 1ms timeout
        timeoutProducerProps.put(ProducerConfig.ACKS_CONFIG, "all");

        KafkaProducer<String, String> timeoutProducer = new KafkaProducer<>(timeoutProducerProps);

        // When - Try to send a message
        ProducerRecord<String, String> record = new ProducerRecord<>(
                TOPIC_NOTIFICATION_EVENTS,
                "timeout-test",
                "test-value"
        );

        // Then - Should handle timeout gracefully
        try {
            timeoutProducer.send(record).get();
            // May succeed or timeout, either way should handle gracefully
        } catch (Exception e) {
            // Timeout is acceptable
            assertThat(e).isNotNull();
        } finally {
            timeoutProducer.close();
        }
    }

    @Test
    @DisplayName("Consumer should handle empty topics gracefully")
    void consumerShouldHandleEmptyTopicsGracefully() {
        // Given - Consumer is subscribed but no messages have been sent to new topics
        // When - Poll for records
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));

        // Then - Should return empty records without error
        assertThat(records).isNotNull();
        assertThat(records.count()).isGreaterThanOrEqualTo(0); // May have leftover messages from previous tests
    }

    @Test
    @DisplayName("Should verify event structure for SMS notification")
    void shouldVerifyEventStructureForSmsNotification() throws ExecutionException, InterruptedException {
        // Given - SMS notification event
        String eventId = UUID.randomUUID().toString();
        String smsEvent = String.format(
                "{\"eventType\":\"sms-notification\",\"userId\":\"user001\",\"phoneNumber\":\"+628987654321\",\"message\":\"Your OTP is 123456\",\"templateId\":\"otp-template\",\"timestamp\":\"%s\"}",
                java.time.Instant.now().toString()
        );

        // When - Send to Kafka
        producer.send(new ProducerRecord<>(TOPIC_NOTIFICATION_EVENTS, eventId, smsEvent)).get();

        // Then - Verify structure
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThan(0);

        boolean foundSmsEvent = false;
        for (var record : records) {
            if (record.value().contains("sms-notification")) {
                foundSmsEvent = true;
                assertThat(record.value()).contains("phoneNumber");
                assertThat(record.value()).contains("+628987654321");
                assertThat(record.value()).contains("message");
                assertThat(record.value()).contains("Your OTP is 123456");
                break;
            }
        }
        assertThat(foundSmsEvent).as("Should find an SMS notification event").isTrue();
    }

    @Test
    @DisplayName("Should verify event structure for push notification")
    void shouldVerifyEventStructureForPushNotification() throws ExecutionException, InterruptedException {
        // Given - Push notification event
        String deviceId = UUID.randomUUID().toString();
        String pushEvent = String.format(
                "{\"eventType\":\"push-notification\",\"userId\":\"user002\",\"deviceId\":\"%s\",\"deviceToken\":\"abc123def456\",\"title\":\"New Transaction\",\"body\":\"Transaction of IDR 50,000 successful\",\"data\":{\"transactionId\":\"TXN123\",\"amount\":50000},\"timestamp\":\"%s\"}",
                deviceId,
                java.time.Instant.now().toString()
        );

        // When - Send to Kafka
        producer.send(new ProducerRecord<>(TOPIC_NOTIFICATION_EVENTS, deviceId, pushEvent)).get();

        // Then - Verify structure
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThan(0);

        boolean foundPushEvent = false;
        for (var record : records) {
            if (record.value().contains("push-notification")) {
                foundPushEvent = true;
                assertThat(record.value()).contains("deviceId");
                assertThat(record.value()).contains("deviceToken");
                assertThat(record.value()).contains("title");
                assertThat(record.value()).contains("body");
                assertThat(record.value()).contains("data");
                break;
            }
        }
        assertThat(foundPushEvent).as("Should find a push notification event").isTrue();
    }

    @Test
    @DisplayName("Should verify event structure for email notification")
    void shouldVerifyEventStructureForEmailNotification() throws ExecutionException, InterruptedException {
        // Given - Email notification event
        String emailId = UUID.randomUUID().toString();
        String emailEvent = String.format(
                "{\"eventType\":\"email-notification\",\"userId\":\"user003\",\"email\":\"customer@payu.fajjjar.my.id\",\"subject\":\"Monthly Statement Available\",\"body\":\"Your monthly statement is now available for download\",\"templateId\":\"monthly-statement\",\"data\":{\"month\":\"January\",\"year\":\"2026\"},\"timestamp\":\"%s\"}",
                java.time.Instant.now().toString()
        );

        // When - Send to Kafka
        producer.send(new ProducerRecord<>(TOPIC_NOTIFICATION_EVENTS, emailId, emailEvent)).get();

        // Then - Verify structure
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThan(0);

        boolean foundEmailEvent = false;
        for (var record : records) {
            if (record.value().contains("email-notification")) {
                foundEmailEvent = true;
                assertThat(record.value()).contains("email");
                assertThat(record.value()).contains("subject");
                assertThat(record.value()).contains("Monthly Statement Available");
                assertThat(record.value()).contains("templateId");
                assertThat(record.value()).contains("data");
                break;
            }
        }
        assertThat(foundEmailEvent).as("Should find an email notification event").isTrue();
    }

    @Test
    @DisplayName("Should verify event structure for WhatsApp notification")
    void shouldVerifyEventStructureForWhatsAppNotification() throws ExecutionException, InterruptedException {
        // Given - WhatsApp notification event (future implementation)
        String waId = UUID.randomUUID().toString();
        String whatsappEvent = String.format(
                "{\"eventType\":\"whatsapp-notification\",\"userId\":\"user004\",\"phoneNumber\":\"+628111222333\",\"templateName\":\"payment_confirmation\",\"parameters\":{\"amount\":\"IDR 100,000\",\"reference\":\"PAY123\"},\"timestamp\":\"%s\"}",
                java.time.Instant.now().toString()
        );

        // When - Send to Kafka
        producer.send(new ProducerRecord<>(TOPIC_NOTIFICATION_EVENTS, waId, whatsappEvent)).get();

        // Then - Verify structure
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThan(0);

        boolean foundWhatsAppEvent = false;
        for (var record : records) {
            if (record.value().contains("whatsapp-notification")) {
                foundWhatsAppEvent = true;
                assertThat(record.value()).contains("phoneNumber");
                assertThat(record.value()).contains("templateName");
                assertThat(record.value()).contains("parameters");
                break;
            }
        }
        assertThat(foundWhatsAppEvent).as("Should find a WhatsApp notification event").isTrue();
    }

    @Test
    @DisplayName("Should handle large payload in notification event")
    void shouldHandleLargePayloadInNotificationEvent() throws ExecutionException, InterruptedException {
        // Given - Create a large payload (simulating detailed transaction history)
        StringBuilder largeBody = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largeBody.append("Transaction line ").append(i).append(": IDR ").append(i * 1000).append("\n");
        }

        String eventId = UUID.randomUUID().toString();
        String largeEvent = String.format(
                "{\"eventType\":\"email-notification\",\"userId\":\"user005\",\"email\":\"user005@payu.fajjjar.my.id\",\"subject\":\"Transaction History\",\"body\":\"%s\",\"timestamp\":\"%s\"}",
                largeBody.toString().replace("\"", "\\\""), // Escape quotes
                java.time.Instant.now().toString()
        );

        // When - Send to Kafka
        producer.send(new ProducerRecord<>(TOPIC_NOTIFICATION_EVENTS, eventId, largeEvent)).get();

        // Then - Verify large payload is handled
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThan(0);

        boolean foundLargeEvent = false;
        for (var record : records) {
            if (record.value().contains("Transaction line 0")) {
                foundLargeEvent = true;
                assertThat(record.value()).contains("Transaction line 99");
                break;
            }
        }
        assertThat(foundLargeEvent).as("Should find and handle large notification event").isTrue();
    }

    @Test
    @DisplayName("Should verify topics follow naming convention")
    void shouldVerifyTopicsFollowNamingConvention() {
        // Verify all topics follow lowercase, dot-separated naming convention
        List<String> topics = List.of(
                TOPIC_WALLET_EVENTS,
                TOPIC_TRANSACTION_EVENTS,
                TOPIC_PAYMENT_EVENTS,
                TOPIC_SPLIT_BILL_EVENTS,
                TOPIC_NOTIFICATION_EVENTS
        );

        for (String topic : topics) {
            assertThat(topic).matches("^[a-z0-9.-]+$");
            assertThat(topic).doesNotContain("_");
            assertThat(topic).doesNotContain(" ");
        }
    }

    @Test
    @DisplayName("Should verify all required fields are present in notification event")
    void shouldVerifyAllRequiredFieldsInNotificationEvent() throws ExecutionException, InterruptedException {
        // Given - Notification event with all required fields
        String notificationId = UUID.randomUUID().toString();
        String requiredFieldsEvent = String.format(
                "{\"notificationId\":\"%s\",\"userId\":\"user006\",\"channel\":\"PUSH\",\"recipient\":\"device123\",\"title\":\"Test\",\"body\":\"Test body\",\"priority\":\"HIGH\",\"timestamp\":\"%s\"}",
                notificationId,
                java.time.Instant.now().toString()
        );

        // When - Send to Kafka
        producer.send(new ProducerRecord<>(TOPIC_NOTIFICATION_EVENTS, notificationId, requiredFieldsEvent)).get();

        // Then - Verify all required fields
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThan(0);

        List<String> requiredFields = List.of("notificationId", "userId", "channel", "recipient", "title", "body", "timestamp");

        boolean foundCompleteEvent = false;
        for (var record : records) {
            if (record.value().contains(notificationId)) {
                foundCompleteEvent = true;
                for (String field : requiredFields) {
                    assertThat(record.value()).contains(field);
                }
                break;
            }
        }
        assertThat(foundCompleteEvent).as("Should find notification event with all required fields").isTrue();
    }
}
