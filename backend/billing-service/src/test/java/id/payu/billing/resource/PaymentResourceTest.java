package id.payu.billing.resource;

import id.payu.billing.client.WalletClient;
import id.payu.billing.dto.CreatePaymentRequest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@QuarkusTest
@DisplayName("Payment Resource Tests")
class PaymentResourceTest {

    @InjectMock
    @RestClient
    WalletClient walletClient;

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
