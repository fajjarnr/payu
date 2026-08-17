package id.payu.dispute.adapter.web;

import id.payu.dispute.domain.model.Dispute;
import id.payu.dispute.domain.port.in.DisputeUseCase;
import id.payu.dispute.interfaces.dto.OpenDisputeRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(DisputeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(DisputeControllerSecurityTest.TestSecurityConfiguration.class)
class DisputeControllerSecurityTest {

    private static final UUID OWNER_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
    private static final UUID FORGED_CUSTOMER_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440099");
    private static final UUID TRANSACTION_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID DISPUTE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440003");
    private static final UUID MERCHANT_ID = UUID.fromString("770e8400-e29b-41d4-a716-446655440002");

    @TestConfiguration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfiguration {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                    .build();
        }
    }

    @MockitoBean
    private DisputeUseCase disputeUseCase;

    @org.springframework.beans.factory.annotation.Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "660e8400-e29b-41d4-a716-446655440001", roles = "USER")
    void userCannotOpenDisputeForAnotherCustomer() throws Exception {
        Dispute dispute = Dispute.create(TRANSACTION_ID, OWNER_ID, MERCHANT_ID,
                new BigDecimal("100000.00"), "IDR", "Product not received");
        when(disputeUseCase.openDispute(any(), any(), any(), any(), any(), any())).thenReturn(dispute);

        OpenDisputeRequest request = OpenDisputeRequest.builder()
                .transactionId(TRANSACTION_ID)
                .customerId(FORGED_CUSTOMER_ID)
                .merchantId(MERCHANT_ID)
                .disputedAmount(new BigDecimal("100000.00"))
                .currency("IDR")
                .reason("Product not received")
                .build();

        mockMvc.perform(post("/api/v1/disputes")
                        .header("Idempotency-Key", "security-dispute-open")
                        .contentType("application/json")
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(disputeUseCase).openDispute(
                eq(TRANSACTION_ID), eq(OWNER_ID), eq(MERCHANT_ID),
                eq(new BigDecimal("100000.00")), eq("IDR"), eq("Product not received"));
    }

    @Test
    @WithMockUser(username = "660e8400-e29b-41d4-a716-446655440001", roles = "USER")
    void userCannotReadAnotherCustomersDisputeById() throws Exception {
        when(disputeUseCase.getDisputeForCustomer(DISPUTE_ID, OWNER_ID)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/v1/disputes/{disputeId}", DISPUTE_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "660e8400-e29b-41d4-a716-446655440001", roles = "USER")
    void userCannotReadAnotherCustomersDisputesByTransaction() throws Exception {
        when(disputeUseCase.getDisputesByTransactionForCustomer(TRANSACTION_ID, OWNER_ID))
                .thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/disputes/transaction/{transactionId}", TRANSACTION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    @WithMockUser(username = "660e8400-e29b-41d4-a716-446655440001", roles = "USER")
    void userCannotQueryAnotherCustomer() throws Exception {
        mockMvc.perform(get("/api/v1/disputes/customer/{customerId}", FORGED_CUSTOMER_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "660e8400-e29b-41d4-a716-446655440001", roles = "USER")
    void userCannotAddEvidenceToAnotherCustomersDispute() throws Exception {
        when(disputeUseCase.getDisputeForCustomer(DISPUTE_ID, OWNER_ID)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(post("/api/v1/disputes/{disputeId}/evidence", DISPUTE_ID)
                        .contentType("application/json")
                        .content("{\"fileName\":\"receipt.pdf\",\"fileUrl\":\"https://storage.example/evidence/receipt.pdf\",\"uploadedBy\":\"CUSTOMER\"}"))
                .andExpect(status().isNotFound());
    }

}
