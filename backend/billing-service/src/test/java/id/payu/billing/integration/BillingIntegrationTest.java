package id.payu.billing.integration;

import id.payu.billing.adapter.client.WalletClient;
import id.payu.billing.dto.CreatePaymentRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import id.payu.outbox.service.OutboxService;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Integration tests for Billing Service.
 *
 * NOTE: These tests require Docker to be running for PostgreSQL Testcontainers.
 * To run these tests: mvn test -Dtest=BillingIntegrationTest -Ddocker.enabled=true
 * To skip these tests: mvn test (they will be skipped by default)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "docker.enabled", matches = "true", disabledReason = "Docker not available")
public class BillingIntegrationTest {

    @LocalServerPort
    int port;

    @MockBean
    WalletClient walletClient;

    @MockBean
    OutboxService outboxService;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void testCreatePaymentFlow() {
        // Mock Wallet Service response
        WalletClient.ReserveResponse mockResponse = new WalletClient.ReserveResponse(
                "res-123", "ACC-001", "REF-BILL-001", "RESERVED"
        );

        Mockito.when(walletClient.reserveBalance(Mockito.anyString(), Mockito.any()))
                .thenReturn(mockResponse);

        // Prepare Request
        // Correct order: accountId, billerCode, customerId, amount
        CreatePaymentRequest request = new CreatePaymentRequest(
                "ACC-001", "PLN", "1234567890", new BigDecimal("50000")
        );

        // 1. Call API to create payment
        String paymentId = given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/payments")
        .then()
                .statusCode(201)
                .body("status", equalTo("COMPLETED"))
                .body("totalAmount", equalTo(52500))
                .body("id", notNullValue())
                .extract().path("id");

        // 2. Verify Database Persistence (via API GET /id)
        given()
                .when().get("/api/v1/payments/" + paymentId)
                .then().statusCode(200)
                .body("status", equalTo("COMPLETED"))
                .body("id", equalTo(paymentId));

        // 3. Verify Outbox Event Created
        Mockito.verify(outboxService, Mockito.timeout(5000)).createEvent(
                Mockito.eq("BillPaymentEntity"),
                Mockito.anyString(),
                Mockito.eq("PaymentCompleted"),
                Mockito.any(Map.class),
                Mockito.isNull(),
                Mockito.eq("payment-events")
        );
    }
}
