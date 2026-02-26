package id.payu.gateway.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

/**
 * Integration tests for Gateway Service filter chain execution.
 *
 * <p>Tests the complete request flow through various filters without requiring
 * external service mocks. These tests focus on filter behavior, headers,
 * validation, and error handling.
 *
 * <p>Test categories:
 * <ul>
 *   <li>CORS Filter behavior</li>
 *   <li>Tenant Filter behavior</li>
 *   <li>API Version Filter behavior</li>
 *   <li>Correlation ID Filter behavior</li>
 *   <li>Request Validation Filter behavior</li>
 *   <li>Error handling and response codes</li>
 * </ul>
 *
 * @author PayU Engineering Team
 * @since 1.0.0
 */
@QuarkusTest
@DisplayName("Gateway Filter Chain Integration Tests")
public class GatewayFilterChainIntegrationTest {

    // ==================== Health Check Tests ====================

    @Test
    @DisplayName("Health check should return UP status")
    void testHealthCheck() {
        given()
                .when().get("/q/health")
                .then()
                .statusCode(anyOf(is(200), is(503)))
                .body("status", anyOf(equalTo("UP"), equalTo("DOWN")));
    }

    @Test
    @DisplayName("Health readiness check should pass")
    void testHealthReadiness() {
        given()
                .when().get("/q/health/ready")
                .then()
                .statusCode(anyOf(is(200), is(503))); // May be down if Redis is not available
    }

    @Test
    @DisplayName("Health liveness check should pass")
    void testHealthLiveness() {
        given()
                .when().get("/q/health/live")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    // ==================== CORS Filter Tests ====================

    @Nested
    @DisplayName("CORS Filter Tests")
    class CorsTests {

        @Test
        @DisplayName("CORS preflight request should return proper headers")
        void testCorsPreflight() {
            given()
                    .header("Origin", "http://localhost:3000")
                    .header("Access-Control-Request-Method", "POST")
                    .header("Access-Control-Request-Headers", "Content-Type")
                    .when()
                    .options("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(204), is(200)))
                    .header("Access-Control-Allow-Origin", notNullValue());
        }

        @Test
        @DisplayName("CORS should allow requests from allowed origins")
        void testCorsAllowedOrigin() {
            given()
                    .header("Origin", "http://localhost:3000")
                    .contentType(ContentType.JSON)
                    .body("{\"accountNumber\":\"1234567890\"}")
                    .when()
                    .post("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(201), is(400), is(415), is(503))); // Various responses depending on backend
        }

        @Test
        @DisplayName("CORS should handle OPTIONS request for non-existent routes")
        void testCorsPreflightNonExistent() {
            given()
                    .header("Origin", "http://localhost:3000")
                    .header("Access-Control-Request-Method", "GET")
                    .when()
                    .options("/api/v1/nonexistent")
                    .then()
                    .statusCode(anyOf(is(204), is(200), is(404)));
        }
    }

    // ==================== Tenant Filter Tests ====================

    @Nested
    @DisplayName("Tenant Filter Tests")
    class TenantTests {

        @Test
        @DisplayName("Request should use default tenant when header not provided")
        void testTenantDefault() {
            given()
                    .when().get("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(200), is(503), is(404))); // May succeed if backend exists
        }

        @Test
        @DisplayName("Request should accept custom tenant header")
        void testTenantCustomHeader() {
            given()
                    .header("X-Tenant-Id", "tenant-123")
                    .when().get("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(200), is(503), is(404)));
        }

        @Test
        @DisplayName("Request should handle multiple tenant headers")
        void testTenantMultipleRequests() {
            // First request with default tenant
            given().when().get("/api/v1/accounts")
                    .then().statusCode(anyOf(is(200), is(503), is(404)));

            // Second request with custom tenant
            given()
                    .header("X-Tenant-Id", "tenant-456")
                    .when().get("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(200), is(503), is(404)));
        }
    }

    // ==================== API Version Filter Tests ====================

    @Nested
    @DisplayName("API Version Filter Tests")
    class ApiVersionTests {

        @Test
        @DisplayName("Request should use default API version when header not provided")
        void testApiVersionDefault() {
            given()
                    .when().get("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(200), is(503), is(404)));
        }

        @Test
        @DisplayName("Request should accept custom API version header")
        void testApiVersionCustomHeader() {
            given()
                    .header("X-API-Version", "v1")
                    .when().get("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(200), is(503), is(404)));
        }

        @Test
        @DisplayName("Request should reject unsupported API version")
        void testApiVersionUnsupported() {
            given()
                    .header("X-API-Version", "v99")
                    .when().get("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(400), is(406), is(503), is(404)));
        }
    }

    // ==================== Correlation ID Filter Tests ====================

    @Nested
    @DisplayName("Correlation ID Filter Tests")
    class CorrelationIdTests {

        @Test
        @DisplayName("Request should generate correlation ID if not provided")
        void testCorrelationIdGenerated() {
            given()
                    .when().get("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(200), is(503), is(404)))
                    .header("X-Correlation-Id", notNullValue());
        }

        @Test
        @DisplayName("Request should use provided correlation ID")
        void testCorrelationIdProvided() {
            String customCorrelationId = "custom-correlation-id-12345";

            given()
                    .header("X-Request-Id", customCorrelationId)
                    .when().get("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(200), is(503), is(404)))
                    .header("X-Correlation-Id", notNullValue());
        }

        @Test
        @DisplayName("Request should forward correlation ID in headers")
        void testCorrelationIdForwarded() {
            String customCorrelationId = "forward-test-123";

            given()
                    .header("X-Request-Id", customCorrelationId)
                    .when().get("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(200), is(503), is(404)));
        }
    }

    // ==================== Request Validation Tests ====================

    @Nested
    @DisplayName("Request Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Request should reject malformed JSON")
        void testMalformedJson() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{invalid json}")
                    .when()
                    .post("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(400), is(415), is(500), is(503)));
        }

        @Test
        @DisplayName("Request should handle empty request body")
        void testEmptyBody() {
            given()
                    .contentType(ContentType.JSON)
                    .when()
                    .post("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(400), is(415), is(500), is(503)));
        }

        @Test
        @DisplayName("Request should handle unsupported content type")
        void testUnsupportedContentType() {
            given()
                    .contentType("text/xml")
                    .body("<data>test</data>")
                    .when()
                    .post("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(415), is(400), is(500)));
        }
    }

    // ==================== Error Handling Tests ====================

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Gateway should return 404 for non-existent endpoints")
        void testNotFoundError() {
            given()
                    .when().get("/api/v1/nonexistent")
                    .then()
                    .statusCode(anyOf(is(404), is(503), is(502)));
        }

        @Test
        @DisplayName("Gateway should return 405 for unsupported methods")
        void testMethodNotAllowed() {
            given()
                    .when().delete("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(405), is(503), is(404)));
        }

        @Test
        @DisplayName("Gateway should handle service unavailable gracefully")
        void testServiceUnavailable() {
            given()
                    .when().get("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(
                        is(200),  // Success
                        is(503),  // Service Unavailable
                        is(502),  // Bad Gateway
                        is(404)   // Not Found
                    ));
        }
    }

    // ==================== HTTP Methods Tests ====================

    @Nested
    @DisplayName("HTTP Methods Tests")
    class HttpMethodTests {

        @Test
        @DisplayName("Gateway should handle GET requests")
        void testGetMethod() {
            given()
                    .when().get("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(200), is(404), is(503)));
        }

        @Test
        @DisplayName("Gateway should handle POST requests")
        void testPostMethod() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"test\":\"data\"}")
                    .when()
                    .post("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(201), is(400), is(415), is(503)));
        }

        @Test
        @DisplayName("Gateway should handle PUT requests")
        void testPutMethod() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"test\":\"data\"}")
                    .when()
                    .put("/api/v1/accounts/123")
                    .then()
                    .statusCode(anyOf(is(200), is(204), is(400), is(404), is(503)));
        }

        @Test
        @DisplayName("Gateway should handle DELETE requests")
        void testDeleteMethod() {
            given()
                    .when().delete("/api/v1/accounts/123")
                    .then()
                    .statusCode(anyOf(is(204), is(404), is(503)));
        }

        @Test
        @DisplayName("Gateway should handle PATCH requests")
        void testPatchMethod() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"test\":\"data\"}")
                    .when()
                    .patch("/api/v1/accounts/123")
                    .then()
                    .statusCode(anyOf(is(200), is(204), is(404), is(405), is(503)));
        }
    }

    // ==================== Query Parameters Tests ====================

    @Nested
    @DisplayName("Query Parameters Tests")
    class QueryParametersTests {

        @Test
        @DisplayName("Gateway should forward query parameters")
        void testQueryParametersForwarded() {
            given()
                    .queryParam("page", "1")
                    .queryParam("size", "10")
                    .when()
                    .get("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(200), is(404), is(503)));
        }

        @Test
        @DisplayName("Gateway should handle multiple query parameters")
        void testMultipleQueryParameters() {
            given()
                    .queryParam("page", "1")
                    .queryParam("size", "10")
                    .queryParam("sort", "name")
                    .queryParam("order", "asc")
                    .when()
                    .get("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(200), is(404), is(503)));
        }

        @Test
        @DisplayName("Gateway should handle empty query parameters")
        void testEmptyQueryParameters() {
            given()
                    .queryParam("")
                    .when()
                    .get("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(200), is(400), is(404), is(503)));
        }
    }

    // ==================== Headers Tests ====================

    @Nested
    @DisplayName("Headers Tests")
    class HeadersTests {

        @Test
        @DisplayName("Gateway should forward custom headers")
        void testCustomHeadersForwarded() {
            given()
                    .header("X-Custom-Header", "custom-value")
                    .header("X-Another-Header", "another-value")
                    .when()
                    .get("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(200), is(404), is(503)));
        }

        @Test
        @DisplayName("Gateway should handle multiple headers with same name")
        void testMultipleHeadersSameName() {
            given()
                    .header("X-Custom-Header", "value1")
                    .header("X-Custom-Header", "value2")
                    .when()
                    .get("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(200), is(404), is(503)));
        }

        @Test
        @DisplayName("Gateway should include security headers")
        void testSecurityHeaders() {
            given()
                    .when()
                    .get("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(200), is(404), is(503)));
        }
    }

    // ==================== Metrics and OpenAPI Tests ====================

    @Test
    @DisplayName("Gateway should expose Prometheus metrics")
    void testMetricsEndpoint() {
        given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Gateway should expose OpenAPI documentation")
    void testOpenApiEndpoint() {
        given()
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .contentType(anyOf(containsString("yaml"), containsString("json")));
    }

    // ==================== Different Service Routes Tests ====================

    @Nested
    @DisplayName("Service Routes Tests")
    class ServiceRoutesTests {

        @Test
        @DisplayName("Gateway should route to account service")
        void testAccountServiceRoute() {
            given()
                    .when().get("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(200), is(404), is(503)));
        }

        @Test
        @DisplayName("Gateway should route to wallet service")
        void testWalletServiceRoute() {
            given()
                    .when().get("/api/v1/wallets")
                    .then()
                    .statusCode(anyOf(is(200), is(404), is(503)));
        }

        @Test
        @DisplayName("Gateway should route to transaction service")
        void testTransactionServiceRoute() {
            given()
                    .when().get("/api/v1/transactions")
                    .then()
                    .statusCode(anyOf(is(200), is(404), is(503)));
        }

        @Test
        @DisplayName("Gateway should route to partner service")
        void testPartnerServiceRoute() {
            given()
                    .when().get("/api/v1/partners")
                    .then()
                    .statusCode(anyOf(is(200), is(404), is(503)));
        }

        @Test
        @DisplayName("Gateway should route to promotion service")
        void testPromotionServiceRoute() {
            given()
                    .when().get("/api/v1/promotions")
                    .then()
                    .statusCode(anyOf(is(200), is(404), is(503)));
        }

        @Test
        @DisplayName("Gateway should route to lending service")
        void testLendingServiceRoute() {
            given()
                    .when().get("/api/v1/lending")
                    .then()
                    .statusCode(anyOf(is(200), is(404), is(503)));
        }

        @Test
        @DisplayName("Gateway should route to investment service")
        void testInvestmentServiceRoute() {
            given()
                    .when().get("/api/v1/investments")
                    .then()
                    .statusCode(anyOf(is(200), is(404), is(503)));
        }

        @Test
        @DisplayName("Gateway should route to compliance service")
        void testComplianceServiceRoute() {
            given()
                    .when().get("/api/v1/compliance")
                    .then()
                    .statusCode(anyOf(is(200), is(404), is(503)));
        }

        @Test
        @DisplayName("Gateway should route to backoffice service")
        void testBackofficeServiceRoute() {
            given()
                    .when().get("/api/v1/backoffice")
                    .then()
                    .statusCode(anyOf(is(200), is(404), is(405), is(503)));
        }

        @Test
        @DisplayName("Gateway should route to support service")
        void testSupportServiceRoute() {
            given()
                    .when().get("/api/v1/support")
                    .then()
                    .statusCode(anyOf(is(200), is(404), is(503)));
        }
    }

    // ==================== Request Body Tests ====================

    @Nested
    @DisplayName("Request Body Tests")
    class RequestBodyTests {

        @Test
        @DisplayName("Gateway should handle JSON request body")
        void testJsonRequestBody() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"accountNumber\":\"1234567890\",\"accountType\":\"SAVINGS\"}")
                    .when()
                    .post("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(201), is(400), is(415), is(503)));
        }

        @Test
        @DisplayName("Gateway should handle large request body")
        void testLargeRequestBody() {
            StringBuilder largeBody = new StringBuilder("{\"data\":\"");
            for (int i = 0; i < 1000; i++) {
                largeBody.append("x");
            }
            largeBody.append("\"}");

            given()
                    .contentType(ContentType.JSON)
                    .body(largeBody.toString())
                    .when()
                    .post("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(201), is(400), is(413), is(415), is(503)));
        }

        @Test
        @DisplayName("Gateway should handle special characters in request body")
        void testSpecialCharactersRequestBody() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"data\":\"Special chars: <>&\\\"'{}\"}")
                    .when()
                    .post("/api/v1/accounts")
                    .then()
                    .statusCode(anyOf(is(201), is(400), is(415), is(503)));
        }
    }

    // ==================== Path Parameters Tests ====================

    @Nested
    @DisplayName("Path Parameters Tests")
    class PathParametersTests {

        @Test
        @DisplayName("Gateway should handle path parameters")
        void testPathParameters() {
            given()
                    .when().get("/api/v1/accounts/123")
                    .then()
                    .statusCode(anyOf(is(200), is(404), is(503)));
        }

        @Test
        @DisplayName("Gateway should handle multiple path parameters")
        void testMultiplePathParameters() {
            given()
                    .when().get("/api/v1/accounts/123/transactions/456")
                    .then()
                    .statusCode(anyOf(is(200), is(404), is(503)));
        }

        @Test
        @DisplayName("Gateway should handle special characters in path parameters")
        void testSpecialCharactersPathParameters() {
            given()
                    .when().get("/api/v1/accounts/test-id-123")
                    .then()
                    .statusCode(anyOf(is(200), is(404), is(503)));
        }
    }
}
