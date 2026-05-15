package id.payu.portal.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Tests for CorrelationIdFilter behavior.
 *
 * Validates that X-Correlation-Id header is propagated correctly
 * in JAX-RS request responses. Note: non-JAX-RS endpoints
 * (like /q/health, /q/openapi) are NOT filtered by JAX-RS filters.
 */
@QuarkusTest
@TestSecurity(authorizationEnabled = false)
@DisplayName("CorrelationIdFilter Tests")
@Tag("infrastructure")
class CorrelationIdFilterTest {

    private static final String PORTAL_SERVICES_PATH = "/api/v1/portal/services";
    private static final String CORRELATION_ID = "X-Correlation-Id";

    @Test
    @DisplayName("should generate correlation ID when none provided")
    void testGeneratesCorrelationIdWhenMissing() {
        given()
            .when().get(PORTAL_SERVICES_PATH)
            .then()
            .statusCode(200)
            .header(CORRELATION_ID, notNullValue())
            .header(CORRELATION_ID, matchesPattern("[0-9a-f]{32}")); // UUID without hyphens
    }

    @Test
    @DisplayName("should echo back provided correlation ID")
    void testEchoesProvidedCorrelationId() {
        String customId = "test-correlation-id-12345";

        given()
            .header(CORRELATION_ID, customId)
            .when().get(PORTAL_SERVICES_PATH)
            .then()
            .statusCode(200)
            .header(CORRELATION_ID, equalTo(customId));
    }

    @Test
    @DisplayName("should include correlation ID for sandbox endpoints")
    void testCorrelationIdOnSandboxEndpoints() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "partnerReferenceNo": "CORR-TEST-001",
                  "amount": { "value": 5000.00, "currency": "IDR" },
                  "beneficiaryAccountNo": "1111111111",
                  "beneficiaryBankCode": "014",
                  "sourceAccountNo": "9999999999"
                }
                """)
            .when().post("/api/v1/sandbox/payments")
            .then()
            .statusCode(200)
            .header(CORRELATION_ID, notNullValue());
    }

    @Test
    @DisplayName("should include correlation ID for portal endpoints")
    void testCorrelationIdOnPortalEndpoints() {
        given()
            .when().get(PORTAL_SERVICES_PATH)
            .then()
            .statusCode(200)
            .header(CORRELATION_ID, notNullValue());
    }

    @Test
    @DisplayName("should include correlation ID for Swagger UI pages")
    void testCorrelationIdOnSwaggerUi() {
        // SwaggerUiResource is @PermitAll JAX-RS resource
        given()
            .when().get("/")
            .then()
            .statusCode(200)
            .header(CORRELATION_ID, notNullValue());
    }

    @Test
    @DisplayName("should generate unique IDs for sequential requests")
    void testGeneratesUniqueIdsForSequentialRequests() {
        String id1 = given()
            .when().get(PORTAL_SERVICES_PATH)
            .then().statusCode(200)
            .extract().header(CORRELATION_ID);

        String id2 = given()
            .when().get(PORTAL_SERVICES_PATH)
            .then().statusCode(200)
            .extract().header(CORRELATION_ID);

        assertNotEquals(id1, id2,
            "Sequential requests must have unique correlation IDs");
    }

    @Test
    @DisplayName("should generate new UUID when blank header is provided")
    void testGeneratesUuidWhenBlankHeaderProvided() {
        // Blank/whitespace header should be treated as missing, generating a new UUID
        String responseId = given()
            .header(CORRELATION_ID, "   ")  // whitespace-only
            .when().get(PORTAL_SERVICES_PATH)
            .then()
            .statusCode(200)
            .header(CORRELATION_ID, notNullValue())
            .extract().header(CORRELATION_ID);

        // Should be a 32-char hex UUID (not the whitespace we sent)
        assertEquals(32, responseId.length(),
            "Generated correlation ID should be 32 hex characters");
        assertNotEquals("   ", responseId,
            "Blank header should trigger new UUID generation");
    }

    // JUnit helper - assertEquals variant with message
    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + " ==> expected: <" + expected + "> but was: <" + actual + ">");
        }
    }
}
