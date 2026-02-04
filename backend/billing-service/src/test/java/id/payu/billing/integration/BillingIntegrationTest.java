package id.payu.billing.integration;

import id.payu.billing.client.WalletClient;
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
import org.springframework.kafka.core.KafkaTemplate;
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
    KafkaTemplate<String, Object> kafkaTemplate;

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

        // 3. Verify Kafka Event
        ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(kafkaTemplate, Mockito.timeout(5000)).send(Mockito.eq("payment-events"), Mockito.eq("ACC-001"), eventCaptor.capture());
 
        Map<String, Object> event = eventCaptor.getValue();
        Assertions.assertEquals("PLN", event.get("billerCode"));
        Assertions.assertEquals("COMPLETED", event.get("status"));
        Assertions.assertEquals("ACC-001", event.get("accountId"));
    }
}
