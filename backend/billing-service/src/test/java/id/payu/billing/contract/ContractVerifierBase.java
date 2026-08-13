package id.payu.billing.contract;

import id.payu.billing.application.service.PaymentService;
import id.payu.billing.domain.model.BillPayment;
import id.payu.billing.domain.model.BillerType;
import id.payu.billing.domain.model.PaymentStatus;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.BDDMockito.given;

/**
 * QAMVP-003: base class for billing-service Spring Cloud Contract verifier
 * tests. Authenticated JWT + mocked PaymentService (no external systems).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureMessageVerifier
@ActiveProfiles("test")
public abstract class ContractVerifierBase {

    protected static final String ACCOUNT_ID = "acct-001";

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected JwtDecoder jwtDecoder;

    @MockitoBean
    protected PaymentService paymentService;

    protected static final java.util.UUID PAYMENT_ID = java.util.UUID.fromString(
            "550e8400-e29b-41d4-a716-446655440000");

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);

        Jwt jwt = Jwt.withTokenValue("contract-token")
                .header("alg", "none")
                .claim("account_id", ACCOUNT_ID)
                .claim("sub", ACCOUNT_ID)
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        BillPayment payment = org.mockito.Mockito.mock(BillPayment.class);
        given(payment.getId()).willReturn(PAYMENT_ID);
        given(payment.getReferenceNumber()).willReturn("REF-001");
        given(payment.getAccountId()).willReturn(ACCOUNT_ID);
        given(payment.getBillerType()).willReturn(BillerType.PLN);
        given(payment.getCustomerId()).willReturn("CUST-1");
        given(payment.getAmount()).willReturn(new BigDecimal("10000"));
        given(payment.getAdminFee()).willReturn(BigDecimal.ZERO);
        given(payment.getTotalAmount()).willReturn(new BigDecimal("10000"));
        given(payment.getStatus()).willReturn(PaymentStatus.COMPLETED);
        given(payment.getCreatedAt()).willReturn(LocalDateTime.now());
        given(payment.getCompletedAt()).willReturn(LocalDateTime.now());
        given(paymentService.getPayment(org.mockito.ArgumentMatchers.any()))
                .willReturn(Optional.empty());
        given(paymentService.getPayment(PAYMENT_ID)).willReturn(Optional.of(payment));
    }

}
