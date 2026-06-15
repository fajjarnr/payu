package id.payu.billing.resource;

import id.payu.billing.domain.port.out.BillerPort;
import id.payu.billing.domain.port.out.PaymentEventPort;
import id.payu.billing.domain.port.out.WalletPort;
import id.payu.commons.idempotency.IdempotencyEntry;
import id.payu.commons.idempotency.IdempotencyKey;
import id.payu.commons.idempotency.IdempotencyRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

@Disabled("Pre-existing test infra issue uncovered after READY-036 cascade fix. See: READY-038 spring-grpc 1.x migration, READY-044 Quarkus REST auth, READY-055 test infra (Redis/Docker/Groovy)")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Top-up Resource Tests")
class TopUpResourceTest {

    @LocalServerPort
    int port;

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

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        Mockito.when(idempotencyRepository.findByKey(any(IdempotencyKey.class)))
                .thenReturn(Optional.empty());
        Mockito.when(idempotencyRepository.saveIfAbsent(any(IdempotencyKey.class), any(IdempotencyEntry.class), anyLong()))
                .thenReturn(true);
        Mockito.doNothing().when(idempotencyRepository).update(any(IdempotencyKey.class), any(IdempotencyEntry.class), anyLong());
    }

    private RequestSpecification givenAuth(String accountId) {
        Jwt mockJwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .claim("account_id", accountId)
                .build();
        Mockito.when(jwtDecoder.decode("test-token")).thenReturn(mockJwt);
        return given()
                .header("Authorization", "Bearer test-token")
                .header("Idempotency-Key", UUID.randomUUID().toString());
    }

    @Test
    @DisplayName("POST /api/v1/topup - should create GoPay top-up")
    void shouldCreateGoPayTopUp() {
        Mockito.when(walletPort.reserveBalance(anyString(), any(BigDecimal.class), anyString()))
            .thenReturn(new WalletPort.ReserveResult("res-123", "RESERVED"));
        Mockito.when(billerPort.pay(anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenReturn(new BillerPort.PaymentResult("00", "Success", "btx-123", "COMPLETED", Instant.now()));

        givenAuth("ACC-001")
            .contentType(ContentType.JSON)
            .body("""
                {
                    "accountId": "ACC-001",
                    "provider": "GOPAY",
                    "walletNumber": "08123456789",
                    "amount": 100000
                }
                """)
            .when()
            .post("/api/v1/topup")
            .then()
            .statusCode(200)
            .body("data.referenceNumber", startsWith("BILL"))
            .body("data.provider", equalTo("GOPAY"))
            .body("data.walletNumber", equalTo("08123456789"))
            .body("data.amount", equalTo(100000))
            .body("data.adminFee", equalTo(1000))
            .body("data.totalAmount", equalTo(101000))
            .body("data.status", equalTo("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/topup - should create OVO top-up")
    void shouldCreateOVOTopUp() {
        Mockito.when(walletPort.reserveBalance(anyString(), any(BigDecimal.class), anyString()))
            .thenReturn(new WalletPort.ReserveResult("res-456", "RESERVED"));
        Mockito.when(billerPort.pay(anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenReturn(new BillerPort.PaymentResult("00", "Success", "btx-456", "COMPLETED", Instant.now()));

        givenAuth("ACC-002")
            .contentType(ContentType.JSON)
            .body("""
                {
                    "accountId": "ACC-002",
                    "provider": "OVO",
                    "walletNumber": "08987654321",
                    "amount": 50000
                }
                """)
            .when()
            .post("/api/v1/topup")
            .then()
            .statusCode(200)
            .body("data.provider", equalTo("OVO"))
            .body("data.walletNumber", equalTo("08987654321"))
            .body("data.amount", equalTo(50000))
            .body("data.adminFee", equalTo(1000))
            .body("data.status", equalTo("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/topup - should create DANA top-up")
    void shouldCreateDNATopUp() {
        Mockito.when(walletPort.reserveBalance(anyString(), any(BigDecimal.class), anyString()))
            .thenReturn(new WalletPort.ReserveResult("res-789", "RESERVED"));
        Mockito.when(billerPort.pay(anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenReturn(new BillerPort.PaymentResult("00", "Success", "btx-789", "COMPLETED", Instant.now()));

        givenAuth("ACC-003")
            .contentType(ContentType.JSON)
            .body("""
                {
                    "accountId": "ACC-003",
                    "provider": "DANA",
                    "walletNumber": "08555555555",
                    "amount": 300000
                }
                """)
            .when()
            .post("/api/v1/topup")
            .then()
            .statusCode(200)
            .body("data.provider", equalTo("DANA"))
            .body("data.walletNumber", equalTo("08555555555"))
            .body("data.amount", equalTo(300000))
            .body("data.adminFee", equalTo(1500))
            .body("data.totalAmount", equalTo(301500))
            .body("data.status", equalTo("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/topup - should create LinkAja top-up")
    void shouldCreateLinkAjaTopUp() {
        Mockito.when(walletPort.reserveBalance(anyString(), any(BigDecimal.class), anyString()))
            .thenReturn(new WalletPort.ReserveResult("res-999", "RESERVED"));
        Mockito.when(billerPort.pay(anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenReturn(new BillerPort.PaymentResult("00", "Success", "btx-999", "COMPLETED", Instant.now()));

        givenAuth("ACC-004")
            .contentType(ContentType.JSON)
            .body("""
                {
                    "accountId": "ACC-004",
                    "provider": "LINKAJA",
                    "walletNumber": "08777777777",
                    "amount": 1000000
                }
                """)
            .when()
            .post("/api/v1/topup")
            .then()
            .statusCode(200)
            .body("data.provider", equalTo("LINKAJA"))
            .body("data.walletNumber", equalTo("08777777777"))
            .body("data.amount", equalTo(1000000))
            .body("data.adminFee", equalTo(2000))
            .body("data.totalAmount", equalTo(1002000))
            .body("data.status", equalTo("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/topup - should fail for unknown provider")
    void shouldFailForUnknownProvider() {
        givenAuth("ACC-001")
            .contentType(ContentType.JSON)
            .body("""
                {
                    "accountId": "ACC-001",
                    "provider": "UNKNOWN",
                    "walletNumber": "08123456789",
                    "amount": 100000
                }
                """)
            .when()
            .post("/api/v1/topup")
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("POST /api/v1/topup - should validate minimum amount")
    void shouldValidateMinimumAmount() {
        givenAuth("ACC-001")
            .contentType(ContentType.JSON)
            .body("""
                {
                    "accountId": "ACC-001",
                    "provider": "GOPAY",
                    "walletNumber": "08123456789",
                    "amount": 5000
                }
                """)
            .when()
            .post("/api/v1/topup")
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("POST /api/v1/topup - should validate maximum amount")
    void shouldValidateMaximumAmount() {
        givenAuth("ACC-001")
            .contentType(ContentType.JSON)
            .body("""
                {
                    "accountId": "ACC-001",
                    "provider": "GOPAY",
                    "walletNumber": "08123456789",
                    "amount": 5000000
                }
                """)
            .when()
            .post("/api/v1/topup")
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("POST /api/v1/topup - should validate wallet number length")
    void shouldValidateWalletNumberLength() {
        givenAuth("ACC-001")
            .contentType(ContentType.JSON)
            .body("""
                {
                    "accountId": "ACC-001",
                    "provider": "GOPAY",
                    "walletNumber": "081234567",
                    "amount": 100000
                }
                """)
            .when()
            .post("/api/v1/topup")
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("POST /api/v1/topup - should validate required fields")
    void shouldValidateRequiredFields() {
        givenAuth("ACC-001")
            .contentType(ContentType.JSON)
            .body("""
                {
                    "accountId": "ACC-001",
                    "provider": "GOPAY"
                }
                """)
            .when()
            .post("/api/v1/topup")
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("GET /api/v1/topup/providers - should return available providers")
    void shouldReturnAvailableProviders() {
        givenAuth("ACC-001")
            .when()
            .get("/api/v1/topup/providers")
            .then()
            .statusCode(200)
            .body("data.size()", equalTo(4))
            .body("data[0].code", equalTo("GOPAY"))
            .body("data[0].name", equalTo("GoPay"))
            .body("data[1].code", equalTo("OVO"))
            .body("data[1].name", equalTo("OVO"))
            .body("data[2].code", equalTo("DANA"))
            .body("data[2].name", equalTo("DANA"))
            .body("data[3].code", equalTo("LINKAJA"))
            .body("data[3].name", equalTo("LinkAja"));
    }

    @Test
    @DisplayName("GET /api/v1/topup/{id} - should return 500 for non-existent top-up")
    void shouldReturn500ForNonExistentTopUp() {
        givenAuth("ACC-001")
            .when()
            .get("/api/v1/topup/00000000-0000-0000-0000-000000000000")
            .then()
            .statusCode(500);
    }
}
