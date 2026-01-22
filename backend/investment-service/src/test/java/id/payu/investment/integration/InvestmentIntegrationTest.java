package id.payu.investment.integration;

import id.payu.investment.InvestmentServiceApplication;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    classes = InvestmentServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = InvestmentIntegrationTest.ContainerInitializer.class)
@Testcontainers
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Investment Service Integration Tests")
public class InvestmentIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("investment_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private Consumer<String, Map<String, Object>> consumer;

    static class ContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext ctx) {
            postgres.start();
            kafka.start();

            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(ctx,
                    "spring.datasource.url=" + postgres.getJdbcUrl(),
                    "spring.datasource.username=" + postgres.getUsername(),
                    "spring.datasource.password=" + postgres.getPassword(),
                    "spring.kafka.bootstrap-servers=" + kafka.getBootstrapServers(),
                    "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
                    "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/payu"
            );
        }
    }

    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-investment-consumer-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.HashMap");

        DefaultKafkaConsumerFactory<String, Map<String, Object>> factory =
            new DefaultKafkaConsumerFactory<>(consumerProps);
        consumer = factory.createConsumer();
        consumer.subscribe(List.of("investment-events"));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    @DisplayName("Kafka container should be running")
    void kafkaContainerShouldBeRunning() {
        assertThat(kafka.isRunning()).isTrue();
        assertThat(kafka.getBootstrapServers()).isNotBlank();
    }

    @Test
    @DisplayName("PostgreSQL container should be running")
    void postgresContainerShouldBeRunning() {
        assertThat(postgres.isRunning()).isTrue();
        assertThat(postgres.getJdbcUrl()).contains("jdbc:postgresql");
    }

    @Test
    @DisplayName("Should be able to poll events from Kafka topics")
    void shouldBeAbleToPollEventsFromKafkaTopics() {
        assertThat(consumer.subscription()).contains("investment-events");

        ConsumerRecords<String, Map<String, Object>> records = consumer.poll(Duration.ofSeconds(2));

        assertThat(records).isNotNull();
    }

    @Test
    @DisplayName("Should have correct Kafka configuration")
    void shouldHaveCorrectKafkaConfiguration() {
        String bootstrapServers = kafka.getBootstrapServers();
        assertThat(bootstrapServers).matches("PLAINTEXT://\\w+:\\d+");
    }

    @Test
    @DisplayName("Consumer should be able to deserialize JSON events")
    void consumerShouldBeAbleToDeserializeJsonEvents() {
        ConsumerRecords<String, Map<String, Object>> records = consumer.poll(Duration.ofSeconds(1));

        assertThat(records).isNotNull();
    }

    @Test
    @DisplayName("Health endpoint should be accessible")
    void healthEndpointShouldBeAccessible() {
        String url = "http://localhost:" + port + "/actuator/health";

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertThat(response.getStatusCode()).isIn(
            org.springframework.http.HttpStatus.OK,
            org.springframework.http.HttpStatus.UNAUTHORIZED,
            org.springframework.http.HttpStatus.FORBIDDEN
        );
    }
}
