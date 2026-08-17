package id.payu.dispute.integration;

import id.payu.dispute.DisputeServiceApplication;
import id.payu.dispute.domain.model.Dispute;
import id.payu.dispute.domain.model.DisputeResolutionType;
import id.payu.dispute.domain.model.DisputeStatus;
import id.payu.dispute.domain.port.out.DisputePersistencePort;
import id.payu.dispute.interfaces.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import id.payu.dispute.domain.model.TransactionDetails;
import id.payu.dispute.domain.port.out.TransactionLookupPort;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for DisputeController.
 *
 * <p>These tests verify the full HTTP layer with actual database using Testcontainers.</p>
 */
@SpringBootTest(classes = DisputeServiceApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
@Import(DisputeControllerIntegrationTest.TestSecurityConfiguration.class)
@WithMockUser(roles = "DISPUTE_AGENT")
class DisputeControllerIntegrationTest {

    @TestConfiguration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfiguration {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .build();
        }

        @Bean
        @Primary
        TransactionLookupPort transactionLookupPort() {
            return transactionId -> java.util.Optional.of(
                    new TransactionDetails(new BigDecimal("100000.00"), "IDR"));
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("dispute_service")
            .withUsername("payu")
            .withPassword("payu");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl().split("\\?")[0]);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DisputePersistencePort disputePersistencePort;

    private static final UUID TRANSACTION_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID CUSTOMER_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
    private static final UUID MERCHANT_ID = UUID.fromString("770e8400-e29b-41d4-a716-446655440002");

    @BeforeEach
    void setUp() {
        // Clean up before each test
        disputePersistencePort.findAll().forEach(d -> disputePersistencePort.deleteById(d.getId()));
    }

    @Test
    @DisplayName("Should open dispute and retrieve it")
    void shouldOpenDisputeAndRetrieveIt() throws Exception {
        // Create open dispute request
        OpenDisputeRequest request = OpenDisputeRequest.builder()
                .transactionId(TRANSACTION_ID)
                .customerId(CUSTOMER_ID)
                .merchantId(MERCHANT_ID)
                .disputedAmount(new BigDecimal("100000.00"))
                .currency("IDR")
                .reason("Product not received")
                .build();

        // Open dispute
        String responseJson = mockMvc.perform(post("/api/v1/disputes")
                        .header("Idempotency-Key", "integration-dispute-open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value(TRANSACTION_ID.toString()))
                .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.merchantId").value(MERCHANT_ID.toString()))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        DisputeResponse response = objectMapper.readValue(responseJson, DisputeResponse.class);

        // Retrieve dispute by ID
        mockMvc.perform(get("/api/v1/disputes/{disputeId}", response.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.getId().toString()))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @DisplayName("Should start investigation and resolve dispute")
    void shouldStartInvestigationAndResolveDispute() throws Exception {
        // Create dispute
        Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID,
                new BigDecimal("100000.00"), "IDR", "Test dispute");
        dispute = disputePersistencePort.save(dispute);

        // Start investigation
        StartInvestigationRequest investigationRequest = StartInvestigationRequest.builder()
                .investigationId("INV-001")
                .build();

        mockMvc.perform(post("/api/v1/disputes/{disputeId}/investigate", dispute.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(investigationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVESTIGATING"))
                .andExpect(jsonPath("$.investigationId").value("INV-001"));

        // Resolve dispute
        ResolveDisputeRequest resolveRequest = ResolveDisputeRequest.builder()
                .resolutionType("REFUND_CUSTOMER")
                .resolution("Evidence supports customer claim")
                .build();

        mockMvc.perform(post("/api/v1/disputes/{disputeId}/resolve", dispute.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolutionType").value("REFUND_CUSTOMER"));
    }

    @Test
    @DisplayName("Should add evidence to dispute")
    void shouldAddEvidenceToDispute() throws Exception {
        // Create dispute
        Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID,
                new BigDecimal("100000.00"), "IDR", "Test dispute");
        dispute = disputePersistencePort.save(dispute);

        // Add evidence
        AddEvidenceRequest evidenceRequest = AddEvidenceRequest.builder()
                .fileName("receipt.pdf")
                .fileUrl("https://storage.payu.fajjjar.my.id/evidence/receipt.pdf")
                .uploadedBy("CUSTOMER")
                .build();

        mockMvc.perform(post("/api/v1/disputes/{disputeId}/evidence", dispute.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(evidenceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidenceList").isArray())
                .andExpect(jsonPath("$.evidenceList[0].fileName").value("receipt.pdf"));
    }

    @Test
    @DisplayName("Should reject dispute")
    void shouldRejectDispute() throws Exception {
        // Create dispute
        Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID,
                new BigDecimal("100000.00"), "IDR", "Test dispute");
        dispute = disputePersistencePort.save(dispute);

        // Reject dispute
        RejectDisputeRequest rejectRequest = RejectDisputeRequest.builder()
                .rejectionReason("Dispute filed after deadline")
                .build();

        mockMvc.perform(post("/api/v1/disputes/{disputeId}/reject", dispute.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Dispute filed after deadline"));
    }

    @Test
    @DisplayName("Should get disputes by customer")
    void shouldGetDisputesByCustomer() throws Exception {
        // Create disputes
        Dispute dispute1 = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID,
                new BigDecimal("100000.00"), "IDR", "Dispute 1");
        Dispute dispute2 = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID,
                new BigDecimal("50000.00"), "IDR", "Dispute 2");
        disputePersistencePort.save(dispute1);
        disputePersistencePort.save(dispute2);

        mockMvc.perform(get("/api/v1/disputes/customer/{customerId}", CUSTOMER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.disputes").isArray());
    }

    @Test
    @DisplayName("Should return 404 when dispute not found")
    void shouldReturn404WhenDisputeNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/disputes/{disputeId}", nonExistentId))
                .andExpect(status().isNotFound());
    }
}
