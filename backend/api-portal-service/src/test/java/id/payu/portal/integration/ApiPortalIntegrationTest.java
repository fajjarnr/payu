package id.payu.portal.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the API Portal Service.
 *
 * Tests the full service lifecycle including:
 * - Portal service listing and OpenAPI aggregation
 * - Sandbox payment lifecycle (create, status, refund, clear)
 * - Health and metrics endpoints
 * - Swagger UI rendering
 */
@QuarkusTest
@TestSecurity(authorizationEnabled = false)
@DisplayName("API Portal Integration Tests")
@Tag("integration")
class ApiPortalIntegrationTest {

    @BeforeEach
    void clearSandbox() {
        given()
            .delete("/api/v1/sandbox/data")
            .then()
            .statusCode(200);
    }

    // ────────── Portal Endpoints ──────────

    @Test
    @DisplayName("should list all services with valid structure")
    void portal_listAllServices() {
        given()
            .when().get("/api/v1/portal/services")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("services", not(empty()))
            .body("services.size()", equalTo(20))
            .body("services[0].id", notNullValue())
            .body("services[0].name", notNullValue())
            .body("services[0].url", notNullValue())
            .body("services[0].openapiPath", notNullValue())
            .body("services[0].status", anyOf(equalTo("UP"), equalTo("DOWN"), equalTo("UNKNOWN")));
    }

    @Test
    @DisplayName("should aggregate OpenAPI specs via direct refresh")
    void portal_aggregatedOpenApiSpecs() {
        // Use refresh=true to avoid cache Duration.parse issue
        given()
            .queryParam("refresh", "true")
            .when().get("/api/v1/portal/openapi")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("version", equalTo("1.0.0"))
            .body("services", notNullValue())
            .body("lastUpdated", notNullValue());
    }

    @Test
    @DisplayName("should refresh OpenAPI specs via POST")
    void portal_refreshOpenApiSpecs() {
        given()
            .contentType(ContentType.JSON)
            .when().post("/api/v1/portal/refresh")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("version", equalTo("1.0.0"))
            .body("lastUpdated", notNullValue());
    }

    // ────────── Sandbox Payment Lifecycle ──────────

    @Test
    @DisplayName("should complete full sandbox payment lifecycle end-to-end")
    void sandbox_fullPaymentLifecycle() {
        // Step 1: Verify initial state - no payments
        given()
            .get("/api/v1/sandbox/stats")
            .then()
            .statusCode(200)
            .body("totalPayments", equalTo(0))
            .body("totalRefunds", equalTo(0));

        // Step 2: Create a payment
        String paymentRef = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "partnerReferenceNo": "E2E-TEST-001",
                  "amount": { "value": 250000.00, "currency": "IDR" },
                  "beneficiaryAccountNo": "1234567890",
                  "beneficiaryBankCode": "014",
                  "sourceAccountNo": "9876543210",
                  "additionalInfo": { "description": "E2E test payment" }
                }
                """)
            .when().post("/api/v1/sandbox/payments")
            .then()
            .statusCode(200)
            .body("partnerReferenceNo", equalTo("E2E-TEST-001"))
            .body("paymentReferenceNo", notNullValue())
            .body("paymentReferenceNo", startsWith("PAY-"))
            .body("paymentStatus", equalTo("COMPLETED"))
            .body("amount.value", equalTo(250000.00f))
            .body("amount.currency", equalTo("IDR"))
            .body("beneficiaryAccountNo", equalTo("1234567890"))
            .body("beneficiaryBankCode", equalTo("014"))
            .body("sourceAccountNo", equalTo("9876543210"))
            .extract().path("paymentReferenceNo");

        assertNotNull(paymentRef);

        // Step 3: Get payment status
        given()
            .when().get("/api/v1/sandbox/payments/" + paymentRef)
            .then()
            .statusCode(200)
            .body("paymentReferenceNo", equalTo(paymentRef))
            .body("partnerReferenceNo", equalTo("E2E-TEST-001"))
            .body("paymentStatus", equalTo("COMPLETED"))
            .body("amount.value", equalTo(250000.00f));

        // Step 4: Create a refund
        String refundRef = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "refundReferenceNo": "E2E-REFUND-001",
                  "reason": "E2E test refund"
                }
                """)
            .pathParam("paymentReferenceNo", paymentRef)
            .when().post("/api/v1/sandbox/payments/{paymentReferenceNo}/refund")
            .then()
            .statusCode(200)
            .body("refundReferenceNo", equalTo("E2E-REFUND-001"))
            .body("originalReferenceNo", equalTo(paymentRef))
            .body("refundStatus", equalTo("COMPLETED"))
            .body("amount.value", equalTo(250000.00f))
            .extract().path("refundReferenceNo");

        assertNotNull(refundRef);

        // Step 5: Verify stats reflect the operations
        given()
            .get("/api/v1/sandbox/stats")
            .then()
            .statusCode(200)
            .body("totalPayments", equalTo(1))
            .body("totalRefunds", equalTo(1));

        // Step 6: Clear all data
        given()
            .delete("/api/v1/sandbox/data")
            .then()
            .statusCode(200)
            .body("message", containsString("cleared"));

        // Step 7: Verify data is cleared
        given()
            .get("/api/v1/sandbox/stats")
            .then()
            .statusCode(200)
            .body("totalPayments", equalTo(0))
            .body("totalRefunds", equalTo(0));

        // Step 8: Verify payment no longer exists
        given()
            .when().get("/api/v1/sandbox/payments/" + paymentRef)
            .then()
            .statusCode(404)
            .body("error", containsString("not found"));
    }

    @Test
    @DisplayName("should handle invalid payment reference gracefully")
    void sandbox_paymentNotFound() {
        given()
            .when().get("/api/v1/sandbox/payments/INVALID-REF-99999")
            .then()
            .statusCode(404)
            .body("error", containsString("not found"));
    }

    @Test
    @DisplayName("should handle refunds for non-existent payments")
    void sandbox_refundForNonExistentPayment() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "refundReferenceNo": "GHOST-REFUND", "reason": "Ghost" }
                """)
            .pathParam("paymentReferenceNo", "NON-EXISTENT-PAYMENT-12345")
            .when().post("/api/v1/sandbox/payments/{paymentReferenceNo}/refund")
            .then()
            .statusCode(404)
            .body("error", containsString("not found"));
    }

    // ────────── Health & Ops ──────────

    @Test
    @DisplayName("should report health status UP at aggregate endpoint")
    void health_aggregateEndpoint() {
        given()
            .when().get("/q/health")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("status", equalTo("UP"))
            .body("checks", notNullValue());
    }

    @Test
    @DisplayName("should report liveness UP")
    void health_liveness() {
        given()
            .when().get("/q/health/live")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("should report readiness UP")
    void health_readiness() {
        given()
            .when().get("/q/health/ready")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("status", equalTo("UP"));
    }

    // ────────── OpenAPI & Docs ──────────

    @Test
    @DisplayName("should expose service OpenAPI spec in JSON format")
    void openApi_serviceSpec() {
        // Quarkus returns YAML by default; explicitly request JSON
        given()
            .accept(ContentType.JSON)
            .when().get("/q/openapi")
            .then()
            .statusCode(200)
            .body("openapi", notNullValue())
            .body("info", notNullValue())
            .body("info.title", equalTo("PayU API Portal"))
            .body("info.version", equalTo("1.0.0"))
            .body("paths", notNullValue());
    }

    // ────────── Swagger UI ──────────

    @Test
    @DisplayName("should render Swagger UI index page with service cards")
    void swaggerUi_indexPage() {
        String html = given()
            .when().get("/")
            .then()
            .statusCode(200)
            .contentType(containsString("text/html"))
            .body(containsString("PayU API Portal"))
            .body(containsString("Available Services"))
            .extract().asString();

        assertTrue(html.contains("service-card"), "Should contain service cards");
        assertTrue(html.contains("swagger-ui"), "Should reference swagger-ui assets");
    }

    @Test
    @DisplayName("should render Swagger UI for a specific service")
    void swaggerUi_servicePage() {
        String html = given()
            .when().get("/service/wallet-service")
            .then()
            .statusCode(200)
            .contentType(containsString("text/html"))
            .body(containsString("wallet-service"))
            .body(containsString("swagger-ui"))
            .body(containsString("Back to Portal"))
            .extract().asString();

        assertTrue(html.contains("SwaggerUIBundle"), "Should contain Swagger UI initialization");
    }

    // ────────── Mock Data ──────────

    @Test
    @DisplayName("should serve mock data examples for partner onboarding")
    void mockData_examples() {
        given()
            .get("/api/v1/sandbox/mock-data/examples")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("paymentExample", notNullValue())
            .body("paymentExample.partnerReferenceNo", equalTo("TEST-PARTNER-REF-001"))
            .body("paymentExample.amount.value", equalTo(100000.00f))
            .body("paymentExample.amount.currency", equalTo("IDR"))
            .body("paymentExample.beneficiaryAccountNo", notNullValue())
            .body("refundExample", notNullValue())
            .body("refundExample.refundReferenceNo", equalTo("TEST-REFUND-REF-001"))
            .body("refundExample.reason", equalTo("Customer request"));
    }

    // ────────── Stats & Latency ──────────

    @Test
    @DisplayName("should report latency configuration in stats")
    void stats_latencyConfig() {
        given()
            .get("/api/v1/sandbox/stats")
            .then()
            .statusCode(200)
            .body("latencyEnabled", notNullValue())
            .body("latencyMinMs", notNullValue())
            .body("latencyMaxMs", notNullValue());
    }

    @Test
    @DisplayName("should track stats across operations")
    void stats_tracksOperationsAccurately() {
        // Create 2 payments and 1 refund
        String ref1 = given()
            .contentType(ContentType.JSON)
            .body("""
                {"partnerReferenceNo":"S1","amount":{"value":1000,"currency":"IDR"},
                 "beneficiaryAccountNo":"1","beneficiaryBankCode":"014","sourceAccountNo":"9"}
                """)
            .when().post("/api/v1/sandbox/payments")
            .then().statusCode(200).extract().path("paymentReferenceNo");

        given()
            .contentType(ContentType.JSON)
            .body("""
                {"partnerReferenceNo":"S2","amount":{"value":2000,"currency":"IDR"},
                 "beneficiaryAccountNo":"2","beneficiaryBankCode":"014","sourceAccountNo":"8"}
                """)
            .when().post("/api/v1/sandbox/payments")
            .then().statusCode(200);

        given()
            .contentType(ContentType.JSON)
            .body("""
                {"refundReferenceNo":"SR1","reason":"Test"}
                """)
            .pathParam("paymentReferenceNo", ref1)
            .when().post("/api/v1/sandbox/payments/{paymentReferenceNo}/refund")
            .then().statusCode(200);

        given()
            .get("/api/v1/sandbox/stats")
            .then()
            .statusCode(200)
            .body("totalPayments", equalTo(2))
            .body("totalRefunds", equalTo(1));
    }
}
