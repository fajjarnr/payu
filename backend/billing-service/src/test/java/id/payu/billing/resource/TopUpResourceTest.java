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
@DisplayName("Top-up Resource Tests")
class TopUpResourceTest {

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
    @DisplayName("POST /api/v1/topup - should create GoPay top-up")
    void shouldCreateGoPayTopUp() throws Exception {
        Mockito.when(walletPort.reserveBalance(anyString(), any(BigDecimal.class), anyString()))
            .thenReturn(new WalletPort.ReserveResult("res-123", "RESERVED"));
        Mockito.when(billerPort.pay(anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenReturn(new BillerPort.PaymentResult("00", "Success", "btx-123", "COMPLETED", Instant.now()));

        mockAuth("ACC-001");

        mockMvc.perform(post("/api/v1/topup")
                        .header(AUTH_HEADER, AUTH_TOKEN)
                        .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "accountId": "ACC-001",
                                "provider": "GOPAY",
                                "walletNumber": "08123456789",
                                "amount": 100000
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.referenceNumber").value(startsWith("BILL")))
                .andExpect(jsonPath("$.data.provider").value("GOPAY"))
                .andExpect(jsonPath("$.data.walletNumber").value("08123456789"))
                .andExpect(jsonPath("$.data.amount").value(100000))
                .andExpect(jsonPath("$.data.adminFee").value(1000))
                .andExpect(jsonPath("$.data.totalAmount").value(101000))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/topup - should create OVO top-up")
    void shouldCreateOVOTopUp() throws Exception {
        Mockito.when(walletPort.reserveBalance(anyString(), any(BigDecimal.class), anyString()))
            .thenReturn(new WalletPort.ReserveResult("res-456", "RESERVED"));
        Mockito.when(billerPort.pay(anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenReturn(new BillerPort.PaymentResult("00", "Success", "btx-456", "COMPLETED", Instant.now()));

        mockAuth("ACC-002");

        mockMvc.perform(post("/api/v1/topup")
                        .header(AUTH_HEADER, AUTH_TOKEN)
                        .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "accountId": "ACC-002",
                                "provider": "OVO",
                                "walletNumber": "08987654321",
                                "amount": 50000
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("OVO"))
                .andExpect(jsonPath("$.data.walletNumber").value("08987654321"))
                .andExpect(jsonPath("$.data.amount").value(50000))
                .andExpect(jsonPath("$.data.adminFee").value(1000))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/topup - should create DANA top-up")
    void shouldCreateDNATopUp() throws Exception {
        Mockito.when(walletPort.reserveBalance(anyString(), any(BigDecimal.class), anyString()))
            .thenReturn(new WalletPort.ReserveResult("res-789", "RESERVED"));
        Mockito.when(billerPort.pay(anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenReturn(new BillerPort.PaymentResult("00", "Success", "btx-789", "COMPLETED", Instant.now()));

        mockAuth("ACC-003");

        mockMvc.perform(post("/api/v1/topup")
                        .header(AUTH_HEADER, AUTH_TOKEN)
                        .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "accountId": "ACC-003",
                                "provider": "DANA",
                                "walletNumber": "08555555555",
                                "amount": 300000
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("DANA"))
                .andExpect(jsonPath("$.data.walletNumber").value("08555555555"))
                .andExpect(jsonPath("$.data.amount").value(300000))
                .andExpect(jsonPath("$.data.adminFee").value(1500))
                .andExpect(jsonPath("$.data.totalAmount").value(301500))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/topup - should create LinkAja top-up")
    void shouldCreateLinkAjaTopUp() throws Exception {
        Mockito.when(walletPort.reserveBalance(anyString(), any(BigDecimal.class), anyString()))
            .thenReturn(new WalletPort.ReserveResult("res-999", "RESERVED"));
        Mockito.when(billerPort.pay(anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenReturn(new BillerPort.PaymentResult("00", "Success", "btx-999", "COMPLETED", Instant.now()));

        mockAuth("ACC-004");

        mockMvc.perform(post("/api/v1/topup")
                        .header(AUTH_HEADER, AUTH_TOKEN)
                        .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "accountId": "ACC-004",
                                "provider": "LINKAJA",
                                "walletNumber": "08777777777",
                                "amount": 1000000
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("LINKAJA"))
                .andExpect(jsonPath("$.data.walletNumber").value("08777777777"))
                .andExpect(jsonPath("$.data.amount").value(1000000))
                .andExpect(jsonPath("$.data.adminFee").value(2000))
                .andExpect(jsonPath("$.data.totalAmount").value(1002000))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/topup - should fail for unknown provider")
    void shouldFailForUnknownProvider() throws Exception {
        mockAuth("ACC-001");

        mockMvc.perform(post("/api/v1/topup")
                        .header(AUTH_HEADER, AUTH_TOKEN)
                        .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "accountId": "ACC-001",
                                "provider": "UNKNOWN",
                                "walletNumber": "08123456789",
                                "amount": 100000
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/topup - should validate minimum amount")
    void shouldValidateMinimumAmount() throws Exception {
        mockAuth("ACC-001");

        mockMvc.perform(post("/api/v1/topup")
                        .header(AUTH_HEADER, AUTH_TOKEN)
                        .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "accountId": "ACC-001",
                                "provider": "GOPAY",
                                "walletNumber": "08123456789",
                                "amount": 5000
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/topup - should validate maximum amount")
    void shouldValidateMaximumAmount() throws Exception {
        mockAuth("ACC-001");

        mockMvc.perform(post("/api/v1/topup")
                        .header(AUTH_HEADER, AUTH_TOKEN)
                        .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "accountId": "ACC-001",
                                "provider": "GOPAY",
                                "walletNumber": "08123456789",
                                "amount": 5000000
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/topup - should validate wallet number length")
    void shouldValidateWalletNumberLength() throws Exception {
        mockAuth("ACC-001");

        mockMvc.perform(post("/api/v1/topup")
                        .header(AUTH_HEADER, AUTH_TOKEN)
                        .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "accountId": "ACC-001",
                                "provider": "GOPAY",
                                "walletNumber": "081234567",
                                "amount": 100000
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/topup - should validate required fields")
    void shouldValidateRequiredFields() throws Exception {
        mockAuth("ACC-001");

        mockMvc.perform(post("/api/v1/topup")
                        .header(AUTH_HEADER, AUTH_TOKEN)
                        .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "accountId": "ACC-001",
                                "provider": "GOPAY"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/topup/providers - should return available providers")
    void shouldReturnAvailableProviders() throws Exception {
        mockAuth("ACC-001");

        mockMvc.perform(get("/api/v1/topup/providers")
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].code").value("GOPAY"))
                .andExpect(jsonPath("$.data[0].name").value("GoPay"))
                .andExpect(jsonPath("$.data[1].code").value("OVO"))
                .andExpect(jsonPath("$.data[1].name").value("OVO"))
                .andExpect(jsonPath("$.data[2].code").value("DANA"))
                .andExpect(jsonPath("$.data[2].name").value("DANA"))
                .andExpect(jsonPath("$.data[3].code").value("LINKAJA"))
                .andExpect(jsonPath("$.data[3].name").value("LinkAja"));
    }

    @Test
    @DisplayName("GET /api/v1/topup/{id} - should return 500 for non-existent top-up")
    void shouldReturn404ForNonExistentTopUp() throws Exception {
        mockAuth("ACC-001");

        mockMvc.perform(get("/api/v1/topup/00000000-0000-0000-0000-000000000000")
                        .header(AUTH_HEADER, AUTH_TOKEN))
                .andExpect(status().isNotFound());
    }
}
