package id.payu.billing.resource;

import id.payu.billing.client.WalletClient;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Payment Resource Tests")
class PaymentResourceTest {

    @LocalServerPort
    int port;

    @MockBean
    WalletClient walletClient;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("POST /api/v1/payments - should create payment")
    void shouldCreatePayment() {
        // Mock wallet response
        Mockito.when(walletClient.reserveBalance(anyString(), any()))
            .thenReturn(new WalletClient.ReserveResponse("res-123", "ACC-001", "ref-123", "RESERVED"));

        given()
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
            .statusCode(201)
            .body("referenceNumber", startsWith("BILL"))
            .body("billerCode", equalTo("PLN"))
            .body("amount", equalTo(100000))
            .body("adminFee", equalTo(2500))
            .body("totalAmount", equalTo(102500))
            .body("status", equalTo("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/payments - should fail for unknown biller")
    void shouldFailForUnknownBiller() {
        given()
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
            .body("message", containsString("Unknown biller"));
    }

    @Test
    @DisplayName("POST /api/v1/payments - should validate request")
    void shouldValidateRequest() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "accountId": "",
                    "billerCode": "PLN"
                }
                """)
            .when()
            .post("/api/v1/payments")
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("GET /api/v1/payments/{id} - should return 404 for non-existent payment")
    void shouldReturn404ForNonExistentPayment() {
        given()
            .when()
            .get("/api/v1/payments/00000000-0000-0000-0000-000000000000")
            .then()
            .statusCode(404);
    }
}
