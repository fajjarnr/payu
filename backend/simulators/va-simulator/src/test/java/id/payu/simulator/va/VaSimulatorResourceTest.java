package id.payu.simulator.va;

import id.payu.simulator.va.interfaces.dto.VaInquiryRequest;
import id.payu.simulator.va.interfaces.dto.VaPaymentRequest;
import id.payu.simulator.va.interfaces.dto.VaRegistrationRequest;
import id.payu.simulator.va.entity.VirtualAccount;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

/**
 * Integration tests for VA Simulator REST API.
 *
 * Part of E-15 IMP-042: Virtual Account Payment Collection
 */
@QuarkusTest
public class VaSimulatorResourceTest {

    @BeforeEach
    @Transactional
    void setup() {
        // Clean up test data
        VirtualAccount.deleteAll();
    }

    @Test
    void testHealthEndpoint() {
        given()
            .when()
            .get("/api/v1/health")
            .then()
            .statusCode(200)
            .body("status", equalTo("UP"))
            .body("service", equalTo("va-simulator"));
    }

    @Test
    void testRegisterVa() {
        VaRegistrationRequest request = new VaRegistrationRequest(
            "123456789012",
            "BCA",
            "Bank Central Asia",
            "partner-123",
            new BigDecimal("100000.00"),
            "IDR",
            Instant.now().plusSeconds(3600),
            "http://localhost:8083/callback",
            "ext-123",
            "John Doe",
            "Test payment"
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/v1/va/register")
            .then()
            .statusCode(201)
            .body("responseCode", equalTo("00"))
            .body("vaNumber", equalTo("123456789012"))
            .body("bankCode", equalTo("BCA"))
            .body("status", equalTo("PENDING"));
    }

    @Test
    void testInquirySuccess() {
        // First register a VA
        registerTestVa();

        VaInquiryRequest request = new VaInquiryRequest(
            "123456789012",
            "BCA",
            "John Doe",
            "ATM"
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/v1/va/inquiry")
            .then()
            .statusCode(200)
            .body("responseCode", equalTo("00"))
            .body("vaNumber", equalTo("123456789012"))
            .body("status", equalTo("ACTIVE"))
            .body("amount", equalTo(100000.00f));
    }

    @Test
    void testInquiryNotFound() {
        VaInquiryRequest request = new VaInquiryRequest(
            "999999999999",
            "BCA",
            null,
            null
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/v1/va/inquiry")
            .then()
            .statusCode(404)
            .body("responseCode", equalTo("14"));
    }

    @Test
    void testPaymentSuccess() {
        // First register a VA
        registerTestVa();

        VaPaymentRequest request = new VaPaymentRequest(
            "123456789012",
            "BCA",
            new BigDecimal("100000.00"),
            "IDR",
            "9876543210",
            "Jane Doe",
            "ATM",
            "REF123456"
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/v1/va/pay")
            .then()
            .statusCode(200)
            .body("responseCode", equalTo("00"))
            .body("vaNumber", equalTo("123456789012"))
            .body("status", equalTo("PAID"))
            .body("amount", equalTo(100000.00f));
    }

    @Test
    void testPaymentAlreadyPaid() {
        // Register and pay a VA
        registerTestVa();
        payTestVa();

        // Try to pay again
        VaPaymentRequest request = new VaPaymentRequest(
            "123456789012",
            "BCA",
            new BigDecimal("100000.00"),
            "IDR",
            "9876543210",
            "Jane Doe",
            "ATM",
            "REF123457"
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/v1/va/pay")
            .then()
            .statusCode(409)
            .body("responseCode", equalTo("94"));
    }

    @Test
    void testGetVaDetails() {
        // Register a VA
        registerTestVa();

        given()
            .when()
            .get("/api/v1/va/123456789012")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.vaNumber", equalTo("123456789012"))
            .body("data.status", equalTo("PENDING"))
            .body("data.amount", equalTo(100000.00f));
    }

    @Test
    void testGetVaNotFound() {
        given()
            .when()
            .get("/api/v1/va/999999999999")
            .then()
            .statusCode(404)
            .body("success", equalTo(false));
    }

    private void registerTestVa() {
        VaRegistrationRequest request = new VaRegistrationRequest(
            "123456789012",
            "BCA",
            "Bank Central Asia",
            "partner-123",
            new BigDecimal("100000.00"),
            "IDR",
            Instant.now().plusSeconds(3600),
            "http://localhost:8083/callback",
            "ext-123",
            "John Doe",
            "Test payment"
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .post("/api/v1/va/register");
    }

    private void payTestVa() {
        VaPaymentRequest request = new VaPaymentRequest(
            "123456789012",
            "BCA",
            new BigDecimal("100000.00"),
            "IDR",
            "9876543210",
            "Jane Doe",
            "ATM",
            "REF123456"
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .post("/api/v1/va/pay");
    }
}
