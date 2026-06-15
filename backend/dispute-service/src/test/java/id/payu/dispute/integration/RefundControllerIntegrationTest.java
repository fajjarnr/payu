package id.payu.dispute.integration;

import id.payu.dispute.DisputeServiceApplication;
import id.payu.dispute.domain.model.Refund;
import id.payu.dispute.domain.model.RefundStatus;
import id.payu.dispute.domain.port.out.RefundPersistencePort;
import id.payu.dispute.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for RefundController.
 *
 * <p>These tests verify the full HTTP layer with actual database using Testcontainers.</p>
 */
@SpringBootTest(classes = DisputeServiceApplication.class)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
class RefundControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("dispute_service")
            .withUsername("payu")
            .withPassword("payu");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RefundPersistencePort refundPersistencePort;

    private static final UUID TRANSACTION_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @BeforeEach
    void setUp() {
        // Clean up before each test
        refundPersistencePort.findAll().forEach(r -> refundPersistencePort.deleteById(r.getId()));
    }

    @Test
    @DisplayName("Should create partial refund and retrieve it")
    void shouldCreatePartialRefundAndRetrieveIt() throws Exception {
        // Create partial refund request
        CreatePartialRefundRequest request = CreatePartialRefundRequest.builder()
                .transactionId(TRANSACTION_ID)
                .amount(new BigDecimal("50000.00"))
                .currency("IDR")
                .reason("Partial refund for damaged item")
                .build();

        // Create refund
        String responseJson = mockMvc.perform(post("/api/v1/refunds/partial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value(TRANSACTION_ID.toString()))
                .andExpect(jsonPath("$.amount").value(50000.00))
                .andExpect(jsonPath("$.currency").value("IDR"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        RefundResponse response = objectMapper.readValue(responseJson, RefundResponse.class);

        // Retrieve refund by ID
        mockMvc.perform(get("/api/v1/refunds/{refundId}", response.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.getId().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("Should process and complete refund")
    void shouldProcessAndCompleteRefund() throws Exception {
        // Create refund first
        Refund refund = Refund.create(TRANSACTION_ID, new BigDecimal("100000.00"), "IDR", "Test refund");
        refund = refundPersistencePort.save(refund);

        // Process refund
        mockMvc.perform(post("/api/v1/refunds/{refundId}/process", refund.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        // Complete refund
        mockMvc.perform(post("/api/v1/refunds/{refundId}/complete", refund.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Should return 404 when refund not found")
    void shouldReturn404WhenRefundNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/refunds/{refundId}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should get refunds by transaction")
    void shouldGetRefundsByTransaction() throws Exception {
        // Create refunds
        Refund refund1 = Refund.create(TRANSACTION_ID, new BigDecimal("50000.00"), "IDR", "Refund 1");
        Refund refund2 = Refund.create(TRANSACTION_ID, new BigDecimal("30000.00"), "IDR", "Refund 2");
        refundPersistencePort.save(refund1);
        refundPersistencePort.save(refund2);

        mockMvc.perform(get("/api/v1/refunds/transaction/{transactionId}", TRANSACTION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.refunds").isArray());
    }

    @Test
    @DisplayName("Should fail refund with reason")
    void shouldFailRefundWithReason() throws Exception {
        // Create and process refund
        Refund refund = Refund.create(TRANSACTION_ID, new BigDecimal("100000.00"), "IDR", "Test refund");
        refund.process();
        refund = refundPersistencePort.save(refund);

        FailRefundRequest request = FailRefundRequest.builder()
                .failureReason("Insufficient funds")
                .build();

        mockMvc.perform(post("/api/v1/refunds/{refundId}/fail", refund.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("Insufficient funds"));
    }

    @Test
    @DisplayName("Should cancel pending refund")
    void shouldCancelPendingRefund() throws Exception {
        // Create refund
        Refund refund = Refund.create(TRANSACTION_ID, new BigDecimal("100000.00"), "IDR", "Test refund");
        refund = refundPersistencePort.save(refund);

        CancelRefundRequest request = CancelRefundRequest.builder()
                .cancellationReason("Customer changed mind")
                .build();

        mockMvc.perform(post("/api/v1/refunds/{refundId}/cancel", refund.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
