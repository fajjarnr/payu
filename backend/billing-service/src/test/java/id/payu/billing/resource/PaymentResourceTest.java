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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.MockBean;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Payment Resource Tests")
class PaymentResourceTest {

    @LocalServerPort
    int port;

    @MockBean
    WalletPort walletPort;

    @MockBean
    BillerPort billerPort;

    @MockBean
    PaymentEventPort eventPort;

    @MockBean
    IdempotencyRepository idempotencyRepository;

    @MockBean
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
    @DisplayName("POST /api/v1/payments - should create payment")
    void shouldCreatePayment() {
        Mockito.when(walletPort.reserveBalance(anyString(), any(BigDecimal.class), anyString()))
            .thenReturn(new WalletPort.ReserveResult("res-123", "RESERVED"));
        Mockito.when(billerPort.pay(anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenReturn(new BillerPort.PaymentResult("00", "Success", "btx-123", "COMPLETED", Instant.now()));

        givenAuth("ACC-001")
            .contentType(ContentType.JSON)
            .body("""
                {
                    "accountId": "ACC-001",
                    "billerCode": "PLN",
                    "customerId": "123456789012",
                    "amount": 100000
                }
                """)
            .when()
            .post("/api/v1/payments")
            .then()
            .statusCode(200)
            .body("data.referenceNumber", startsWith("BILL"))
            .body("data.billerCode", equalTo("PLN"))
            .body("data.amount", equalTo(100000))
            .body("data.adminFee", equalTo(2500))
            .body("data.totalAmount", equalTo(102500))
            .body("data.status", equalTo("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/payments - should fail for unknown biller")
    void shouldFailForUnknownBiller() {
        givenAuth("ACC-001")
            .contentType(ContentType.JSON)
            .body("""
                {
                    "accountId": "ACC-001",
                    "billerCode": "UNKNOWN",
                    "customerId": "123456789",
                    "amount": 50000
                }
                """)
            .when()
            .post("/api/v1/payments")
            .then()
            .statusCode(400)
            .body("error.message", containsString("Unknown biller"));
    }

    @Test
    @DisplayName("POST /api/v1/payments - should validate request")
    void shouldValidateRequest() {
        givenAuth("ACC-001")
            .contentType(ContentType.JSON)
            .body("""
                {
                    "accountId": "ACC-001",
                    "billerCode": "PLN"
                }
                """)
            .when()
            .post("/api/v1/payments")
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("GET /api/v1/payments/{id} - should return 500 for non-existent payment")
    void shouldReturn500ForNonExistentPayment() {
        givenAuth("ACC-001")
            .when()
            .get("/api/v1/payments/00000000-0000-0000-0000-000000000000")
            .then()
            .statusCode(500);
    }
}
