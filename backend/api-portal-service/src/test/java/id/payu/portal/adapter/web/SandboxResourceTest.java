package id.payu.portal.adapter.web;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestSecurity(authorizationEnabled = false)
@DisplayName("Sandbox Resource Tests")
class SandboxResourceTest {

    private String createdPaymentReferenceNo;

    @BeforeEach
    void clearData() {
        given()
            .delete("/api/v1/sandbox/data")
            .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("should create a sandbox payment via REST API")
    void testCreatePayment() {
        String requestBody = """
            {
              "partnerReferenceNo": "TEST-PARTNER-REF-001",
              "amount": {
                "value": 100000.00,
                "currency": "IDR"
              },
              "beneficiaryAccountNo": "1234567890",
              "beneficiaryBankCode": "014",
              "sourceAccountNo": "9876543210",
              "additionalInfo": {
                "description": "Test payment"
              }
            }
            """;

        var response = given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/api/v1/sandbox/payments")
            .then()
            .statusCode(200)
            .body("partnerReferenceNo", equalTo("TEST-PARTNER-REF-001"))
            .body("paymentStatus", equalTo("COMPLETED"))
            .body("amount.value", equalTo(100000.00f))
            .body("amount.currency", equalTo("IDR"))
            .body("beneficiaryAccountNo", equalTo("1234567890"))
            .body("beneficiaryBankCode", equalTo("014"))
            .body("sourceAccountNo", equalTo("9876543210"))
            .extract()
            .response();

        createdPaymentReferenceNo = response.path("paymentReferenceNo");
    }

    @Test
    @DisplayName("should retrieve payment status via REST API")
    void testGetPaymentStatus() {
        String requestBody = """
            {
              "partnerReferenceNo": "TEST-REF-002",
              "amount": {
                "value": 50000.00,
                "currency": "IDR"
              },
              "beneficiaryAccountNo": "1234567890",
              "beneficiaryBankCode": "014",
              "sourceAccountNo": "9876543210"
            }
            """;

        var paymentResponse = given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/api/v1/sandbox/payments")
            .then()
            .statusCode(200)
            .extract()
            .response();

        String paymentReferenceNo = paymentResponse.path("paymentReferenceNo");

        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/api/v1/sandbox/payments/" + paymentReferenceNo)
            .then()
            .statusCode(200)
            .body("partnerReferenceNo", equalTo("TEST-REF-002"))
            .body("paymentReferenceNo", equalTo(paymentReferenceNo))
            .body("paymentStatus", equalTo("COMPLETED"))
            .body("amount.value", equalTo(50000.00f))
            .body("amount.currency", equalTo("IDR"));
    }

    @Test
    @DisplayName("should return 404 for non-existent payment")
    void testGetPaymentStatusNotFound() {
        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/api/v1/sandbox/payments/NON-EXISTENT")
            .then()
            .statusCode(404)
            .body("error", containsString("not found"));
    }

    @Test
    @DisplayName("should create a refund for an existing payment")
    void testCreateRefund() {
        String paymentRequestBody = """
            {
              "partnerReferenceNo": "TEST-REF-003",
              "amount": {
                "value": 75000.00,
                "currency": "IDR"
              },
              "beneficiaryAccountNo": "1234567890",
              "beneficiaryBankCode": "014",
              "sourceAccountNo": "9876543210"
            }
            """;

        var paymentResponse = given()
            .contentType(ContentType.JSON)
            .body(paymentRequestBody)
            .when()
            .post("/api/v1/sandbox/payments")
            .then()
            .statusCode(200)
            .extract()
            .response();

        String paymentReferenceNo = paymentResponse.path("paymentReferenceNo");

        String refundRequestBody = """
            {
              "refundReferenceNo": "REFUND-REF-001",
              "reason": "Customer request"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(refundRequestBody)
            .pathParam("paymentReferenceNo", paymentReferenceNo)
            .when()
            .post("/api/v1/sandbox/payments/{paymentReferenceNo}/refund")
            .then()
            .statusCode(200)
            .body("refundReferenceNo", equalTo("REFUND-REF-001"))
            .body("originalReferenceNo", equalTo(paymentReferenceNo))
            .body("refundStatus", equalTo("COMPLETED"))
            .body("amount.value", equalTo(75000.00f))
            .body("amount.currency", equalTo("IDR"));
    }

    @Test
    @DisplayName("should return 404 when refunding non-existent payment")
    void testCreateRefundForNonExistentPayment() {
        String refundRequestBody = """
            {
              "refundReferenceNo": "REFUND-REF-002",
              "reason": "Test refund"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(refundRequestBody)
            .pathParam("paymentReferenceNo", "NON-EXISTENT")
            .when()
            .post("/api/v1/sandbox/payments/{paymentReferenceNo}/refund")
            .then()
            .statusCode(404)
            .body("error", containsString("not found"));
    }

    @Test
    @DisplayName("should clear all sandbox data")
    void testClearData() {
        String paymentRequestBody = """
            {
              "partnerReferenceNo": "TEST-REF-004",
              "amount": {
                "value": 25000.00,
                "currency": "IDR"
              },
              "beneficiaryAccountNo": "1234567890",
              "beneficiaryBankCode": "014",
              "sourceAccountNo": "9876543210"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(paymentRequestBody)
            .when()
            .post("/api/v1/sandbox/payments")
            .then()
            .statusCode(200);

        given()
            .delete("/api/v1/sandbox/data")
            .then()
            .statusCode(200)
            .body("message", containsString("cleared"));

        given()
            .get("/api/v1/sandbox/stats")
            .then()
            .statusCode(200)
            .body("totalPayments", equalTo(0))
            .body("totalRefunds", equalTo(0));
    }

    @Test
    @DisplayName("should return sandbox statistics")
    void testGetStats() {
        given()
            .get("/api/v1/sandbox/stats")
            .then()
            .statusCode(200)
            .body("totalPayments", notNullValue())
            .body("totalRefunds", notNullValue())
            .body("latencyEnabled", notNullValue())
            .body("latencyMinMs", notNullValue())
            .body("latencyMaxMs", notNullValue());
    }

    @Test
    @DisplayName("should return mock data examples")
    void testGetMockDataExamples() {
        given()
            .get("/api/v1/sandbox/mock-data/examples")
            .then()
            .statusCode(200)
            .body("paymentExample", notNullValue())
            .body("refundExample", notNullValue())
            .body("paymentExample.partnerReferenceNo", notNullValue())
            .body("paymentExample.amount", notNullValue())
            .body("refundExample.refundReferenceNo", notNullValue());
    }

    // ────────── NEW TESTS ──────────

    @Test
    @DisplayName("should create payment with minimal request body")
    void testCreatePayment_MinimalRequest() {
        String requestBody = """
            {
              "partnerReferenceNo": "MIN-REF-001",
              "amount": {
                "value": 5000.00,
                "currency": "IDR"
              },
              "beneficiaryAccountNo": "1111111111",
              "beneficiaryBankCode": "014",
              "sourceAccountNo": "9999999999"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/api/v1/sandbox/payments")
            .then()
            .statusCode(200)
            .body("partnerReferenceNo", equalTo("MIN-REF-001"))
            .body("paymentReferenceNo", notNullValue())
            .body("paymentStatus", equalTo("COMPLETED"))
            .body("amount.value", equalTo(5000.00f))
            .body("amount.currency", equalTo("IDR"));
    }

    @Test
    @DisplayName("should complete full payment lifecycle: create -> get -> refund -> stats")
    @Tag("integration")
    void testFullPaymentLifecycle() {
        // 1. Create payment
        String paymentBody = """
            {
              "partnerReferenceNo": "LIFECYCLE-001",
              "amount": {
                "value": 150000.00,
                "currency": "IDR"
              },
              "beneficiaryAccountNo": "5555555555",
              "beneficiaryBankCode": "008",
              "sourceAccountNo": "4444444444"
            }
            """;

        String paymentRef = given()
            .contentType(ContentType.JSON)
            .body(paymentBody)
            .when()
            .post("/api/v1/sandbox/payments")
            .then()
            .statusCode(200)
            .body("partnerReferenceNo", equalTo("LIFECYCLE-001"))
            .body("paymentReferenceNo", notNullValue())
            .body("paymentStatus", equalTo("COMPLETED"))
            .extract().path("paymentReferenceNo");

        // 2. Get payment status
        given()
            .when()
            .get("/api/v1/sandbox/payments/" + paymentRef)
            .then()
            .statusCode(200)
            .body("paymentReferenceNo", equalTo(paymentRef))
            .body("paymentStatus", equalTo("COMPLETED"));

        // 3. Create refund
        String refundBody = """
            {
              "refundReferenceNo": "LIFECYCLE-REFUND-001",
              "reason": "Lifecycle test refund"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(refundBody)
            .pathParam("paymentReferenceNo", paymentRef)
            .when()
            .post("/api/v1/sandbox/payments/{paymentReferenceNo}/refund")
            .then()
            .statusCode(200)
            .body("refundReferenceNo", equalTo("LIFECYCLE-REFUND-001"))
            .body("originalReferenceNo", equalTo(paymentRef))
            .body("refundStatus", equalTo("COMPLETED"))
            .body("amount.value", equalTo(150000.00f));

        // 4. Verify stats reflect operations
        given()
            .get("/api/v1/sandbox/stats")
            .then()
            .statusCode(200)
            .body("totalPayments", equalTo(1))
            .body("totalRefunds", equalTo(1));
    }

    @Test
    @DisplayName("should handle large amount values correctly")
    @Tag("financial")
    void testCreatePayment_LargeAmount() {
        String requestBody = """
            {
              "partnerReferenceNo": "LARGE-REF-001",
              "amount": {
                "value": 999999999.99,
                "currency": "IDR"
              },
              "beneficiaryAccountNo": "1111111111",
              "beneficiaryBankCode": "014",
              "sourceAccountNo": "9999999999"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/api/v1/sandbox/payments")
            .then()
            .statusCode(200)
            .body("amount.value", equalTo(999999999.99f))
            .body("amount.currency", equalTo("IDR"));
    }

    @Test
    @DisplayName("should create multiple payments and verify uniqueness")
    void testMultiplePayments_UniqueReferenceNumbers() {
        String requestBodyTemplate = """
            {
              "partnerReferenceNo": "%s",
              "amount": {
                "value": 10000.00,
                "currency": "IDR"
              },
              "beneficiaryAccountNo": "1111111111",
              "beneficiaryBankCode": "014",
              "sourceAccountNo": "9999999999"
            }
            """;

        String ref1 = given()
            .contentType(ContentType.JSON)
            .body(String.format(requestBodyTemplate, "MULTI-A"))
            .when().post("/api/v1/sandbox/payments")
            .then().statusCode(200).extract().path("paymentReferenceNo");

        String ref2 = given()
            .contentType(ContentType.JSON)
            .body(String.format(requestBodyTemplate, "MULTI-B"))
            .when().post("/api/v1/sandbox/payments")
            .then().statusCode(200).extract().path("paymentReferenceNo");

        assertNotNull(ref1);
        assertNotNull(ref2);
        assertNotEquals(ref1, ref2, "Payment references must be unique");
    }

    @Test
    @DisplayName("should return valid JSON from stats endpoint")
    void testGetStats_ValidJsonStructure() {
        given()
            .get("/api/v1/sandbox/stats")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("totalPayments", instanceOf(Integer.class))
            .body("totalRefunds", instanceOf(Integer.class))
            .body("latencyEnabled", instanceOf(Boolean.class))
            .body("latencyMinMs", instanceOf(Integer.class))
            .body("latencyMaxMs", instanceOf(Integer.class));
    }

    @Test
    @DisplayName("should respond with JSON from mock data examples")
    void testGetMockDataExamples_ValidJson() {
        given()
            .get("/api/v1/sandbox/mock-data/examples")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("paymentExample.partnerReferenceNo", equalTo("TEST-PARTNER-REF-001"))
            .body("paymentExample.amount.value", equalTo(100000.00f))
            .body("paymentExample.amount.currency", equalTo("IDR"))
            .body("refundExample.refundReferenceNo", equalTo("TEST-REFUND-REF-001"))
            .body("refundExample.reason", equalTo("Customer request"));
    }
}
