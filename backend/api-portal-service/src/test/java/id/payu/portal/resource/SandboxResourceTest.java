package id.payu.portal.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.Matchers.*;

@QuarkusTest
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
}
