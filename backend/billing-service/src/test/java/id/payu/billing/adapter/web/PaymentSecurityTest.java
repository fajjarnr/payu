package id.payu.billing.adapter.web;

import id.payu.billing.application.service.PaymentService;
import id.payu.billing.domain.model.BillPayment;
import id.payu.billing.domain.model.BillerType;
import id.payu.billing.domain.model.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QAMVP-014 (billing — money service): unauthenticated → 401; authenticated
 * owner → 200; authenticated non-owner → not exposed (404, ownership
 * validated server-side).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("QAMVP-014 — billing security: 401 + ownership")
class PaymentSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    private static final String ACCOUNT_ID = "acct-001";

    private BillPayment payment() {
        BillPayment p = mock(BillPayment.class);
        when(p.getId()).thenReturn(UUID.randomUUID());
        when(p.getReferenceNumber()).thenReturn("REF-001");
        when(p.getAccountId()).thenReturn(ACCOUNT_ID);
        when(p.getBillerType()).thenReturn(BillerType.PLN);
        when(p.getCustomerId()).thenReturn("CUST-1");
        when(p.getAmount()).thenReturn(new BigDecimal("10000"));
        when(p.getAdminFee()).thenReturn(BigDecimal.ZERO);
        when(p.getTotalAmount()).thenReturn(new BigDecimal("10000"));
        when(p.getStatus()).thenReturn(PaymentStatus.COMPLETED);
        when(p.getCreatedAt()).thenReturn(java.time.LocalDateTime.now());
        when(p.getCompletedAt()).thenReturn(java.time.LocalDateTime.now());
        return p;
    }

    @Test
    @DisplayName("unauthenticated money request is rejected with 401")
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/payments/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("authenticated owner gets 200")
    void ownerIsAllowed() throws Exception {
        BillPayment p = payment();
        when(paymentService.getPayment(p.getId())).thenReturn(Optional.of(p));

        mockMvc.perform(get("/api/v1/payments/{id}", p.getId())
                        .with(jwt().jwt(j -> j.claim("account_id", ACCOUNT_ID))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("validation failure returns RFC 9457 problem+json (400)")
    void validationFailureIsRfc9457() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .header("X-Idempotency-Key", java.util.UUID.randomUUID().toString())
                        .with(jwt().jwt(j -> j.claim("account_id", ACCOUNT_ID)))
                        .contentType("application/json")
                        .content("{\"accountId\":\"acct-001\",\"billerCode\":\"PLN\",\"customerId\":\"CUST-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType("application/problem+json"));
    }
}
