package id.payu.billing.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.billing.domain.port.out.BillerPort;
import id.payu.billing.domain.port.out.PaymentEventPort;
import id.payu.billing.domain.port.out.WalletPort;
import id.payu.commons.idempotency.IdempotencyEntry;
import id.payu.commons.idempotency.IdempotencyKey;
import id.payu.commons.idempotency.IdempotencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Payment Resource Tests")
class PaymentResourceTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String AUTH_TOKEN = "Bearer test-token";
    private static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    WalletPort walletPort;

    @MockitoBean
    BillerPort billerPort;

    @MockitoBean
    PaymentEventPort eventPort;

    @MockitoBean
    IdempotencyRepository idempotencyRepository;

    @MockitoBean
    JwtDecoder jwtDecoder;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Autowired
    private FilterRegistrationBean<?> idempotencyRequestBodyFilter;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(idempotencyRequestBodyFilter.getFilter())
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper();

        Mockito.when(idempotencyRepository.findByKey(any(IdempotencyKey.class)))
                .thenReturn(Optional.empty());
        Mockito.when(idempotencyRepository.saveIfAbsent(any(IdempotencyKey.class), any(IdempotencyEntry.class), anyLong()))
                .thenReturn(true);
        Mockito.doNothing().when(idempotencyRepository).update(any(IdempotencyKey.class), any(IdempotencyEntry.class), anyLong());
    }

    private void mockAuth(String accountId) {
        Jwt mockJwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .claim("account_id", accountId)
                .build();
        Mockito.when(jwtDecoder.decode("test-token")).thenReturn(mockJwt);
    }

    @Test
    @DisplayName("POST /api/v1/payments - should create payment")
    void shouldCreatePayment() throws Exception {
        Mockito.when(walletPort.reserveBalance(anyString(), any(BigDecimal.class), anyString()))
            .thenReturn(new WalletPort.ReserveResult("res-123", "RESERVED"));
        Mockito.when(billerPort.pay(anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenReturn(new BillerPort.PaymentResult("00", "Success", "btx-123", "COMPLETED", Instant.now()));

        mockAuth("ACC-001");

        mockMvc.perform(post("/api/v1/payments")
                        .header(AUTH_HEADER, AUTH_TOKEN)
                        .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "accountId": "ACC-001",
                                "billerCode": "PLN",
                                "customerId": "123456789012",
                                "amount": 100000
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.referenceNumber").value(startsWith("BILL")))
                .andExpect(jsonPath("$.data.billerCode").value("PLN"))
                .andExpect(jsonPath("$.data.amount").value(100000))
                .andExpect(jsonPath("$.data.adminFee").value(2500))
                .andExpect(jsonPath("$.data.totalAmount").value(102500))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/payments - should fail for unknown biller")
    void shouldFailForUnknownBiller() throws Exception {
        mockAuth("ACC-001");

        mockMvc.perform(post("/api/v1/payments")
                        .header(AUTH_HEADER, AUTH_TOKEN)
                        .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "accountId": "ACC-001",
                                "billerCode": "UNKNOWN",
                                "customerId": "123456789",
                                "amount": 50000
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Unknown biller")));
    }

    @Test
    @DisplayName("POST /api/v1/payments - should validate request")
    void shouldValidateRequest() throws Exception {
        mockAuth("ACC-001");

        mockMvc.perform(post("/api/v1/payments")
                        .header(AUTH_HEADER, AUTH_TOKEN)
                        .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "accountId": "ACC-001",
                                "billerCode": "PLN"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/payments/{id} - should return 500 for non-existent payment")
    void shouldReturn500ForNonExistentPayment() throws Exception {
        mockAuth("ACC-001");

        mockMvc.perform(get("/api/v1/payments/00000000-0000-0000-0000-000000000000")
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isInternalServerError());
    }
}
