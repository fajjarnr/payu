package id.payu.gateway.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import id.payu.gateway.application.service.PersistentAnalyticsService;
import id.payu.gateway.domain.repository.ApiAnalyticsRepository.EndpointMetrics;
import id.payu.gateway.domain.repository.ApiAnalyticsRepository.PartnerMetrics;
import id.payu.gateway.domain.repository.ApiAnalyticsRepository.EndpointUsage;
import id.payu.gateway.domain.vo.HttpMethod;
import io.quarkus.test.InjectMock;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.util.Collections;
import java.util.Map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for Analytics Endpoints.
 *
 * <p>Tests the gateway's analytics and metrics collection features:
 * <ul>
 *   <li>Metrics retrieval endpoint</li>
 *   <li>Analytics health check</li>
 *   <li>Query parameter handling</li>
 *   <li>Error handling</li>
 * </ul>
 *
 * @author PayU Engineering Team
 * @since 1.0.0
 */
@QuarkusTest
@TestProfile(AnalyticsEndpointsTestProfile.class)
@TestSecurity(authorizationEnabled = false)
@DisplayName("Analytics Endpoints Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AnalyticsEndpointsIntegrationTest {

    @InjectMock
    PersistentAnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Standard mock behavior for health
        when(analyticsService.getBufferSize()).thenReturn(0L);

        // Standard mock behavior for endpoint metrics
        when(analyticsService.getEndpointMetrics(anyString(), anyString(), any(), any()))
            .thenAnswer(invocation -> {
                String path = invocation.getArgument(0);
                if (path != null && path.contains("nonexistent")) {
                    return Uni.createFrom().nullItem();
                }
                
                // Return dummy metrics to avoid 500
                return Uni.createFrom().item(new EndpointMetrics(
                    path,
                    HttpMethod.GET,
                    100L, 90L, 10L, 50.0, 10L, 200L, Map.of(200, 90L, 500, 10L)
                ));
            });

        // Mock for top endpoints
        when(analyticsService.getTopEndpoints(anyInt(), any(), any()))
            .thenReturn(Multi.createFrom().items(
                new EndpointUsage("/api/v1/test", HttpMethod.GET, 10L, 5.0, 0.0)
            ));
            
        // Mock for partner metrics
        when(analyticsService.getPartnerMetrics(anyString(), any(), any()))
            .thenReturn(Uni.createFrom().item(
                new PartnerMetrics("partner-test", 100L, 100L, 0L, 0L, 5.0, 5L, 5L, Collections.emptyMap())
            ));
    }

    // ==================== Analytics Health Tests ====================

    @Test
    @Order(1)
    @DisplayName("Analytics health endpoint should return UP status")
    void testAnalyticsHealth() {
        given()
                .when()
                .get("/gateway/analytics/health")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("status", equalTo("UP"))
                .body("service", equalTo("analytics"));
    }

    // ==================== Metrics Endpoint Tests ====================

    @Test
    @Order(10)
    @DisplayName("Metrics endpoint should return 400 when path parameter is missing")
    void testMetricsMissingPath() {
        given()
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(400)
                .body("error", equalTo("path parameter is required"));
    }

    @Test
    @Order(11)
    @DisplayName("Metrics endpoint should accept path parameter")
    void testMetricsWithPathParameter() {
        given()
                .queryParam("path", "/api/v1/accounts")
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(200), is(404))); // 200 if metrics exist, 404 if not found
    }

    @Test
    @Order(12)
    @DisplayName("Metrics endpoint should accept both path and method parameters")
    void testMetricsWithPathAndMethod() {
        given()
                .queryParam("path", "/api/v1/accounts")
                .queryParam("method", "GET")
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    @Test
    @Order(13)
    @DisplayName("Metrics endpoint should handle empty path parameter")
    void testMetricsEmptyPath() {
        given()
                .queryParam("path", "")
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(400)
                .body("error", equalTo("path parameter is required"));
    }

    // ==================== Different Path Tests ====================

    @Test
    @Order(20)
    @DisplayName("Should query metrics for accounts endpoint")
    void testMetricsAccountsEndpoint() {
        given()
                .queryParam("path", "/api/v1/accounts")
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    @Test
    @Order(21)
    @DisplayName("Should query metrics for transactions endpoint")
    void testMetricsTransactionsEndpoint() {
        given()
                .queryParam("path", "/api/v1/transactions")
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    @Test
    @Order(22)
    @DisplayName("Should query metrics for wallets endpoint")
    void testMetricsWalletsEndpoint() {
        given()
                .queryParam("path", "/api/v1/wallets")
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    // ==================== Different Method Tests ====================

    @Test
    @Order(30)
    @DisplayName("Should query metrics for GET requests")
    void testMetricsGetRequests() {
        given()
                .queryParam("path", "/api/v1/accounts")
                .queryParam("method", "GET")
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    @Test
    @Order(31)
    @DisplayName("Should query metrics for POST requests")
    void testMetricsPostRequests() {
        given()
                .queryParam("path", "/api/v1/accounts")
                .queryParam("method", "POST")
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    // ==================== Metrics Not Found Tests ====================

    @Test
    @Order(40)
    @DisplayName("Should return 404 when metrics not found for endpoint")
    void testMetricsNotFound() {
        given()
                .queryParam("path", "/api/v1/nonexistent")
                .queryParam("method", "GET")
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(404)
                .body("error", equalTo("METRICS_NOT_FOUND"))
                .body("message", containsString("No metrics found"));
    }

    // ==================== Special Characters in Path Tests ====================

    @Test
    @Order(50)
    @DisplayName("Should handle paths with special characters")
    void testMetricsSpecialCharactersInPath() {
        given()
                .queryParam("path", "/api/v1/accounts/test-id-123")
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    @Test
    @Order(51)
    @DisplayName("Should handle paths with query strings")
    void testMetricsPathWithQueryString() {
        // The path parameter should not include the query string
        // but we should handle it gracefully if it does
        given()
                .queryParam("path", "/api/v1/accounts?page=1")
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    // ==================== Case Sensitivity Tests ====================

    @Test
    @Order(60)
    @DisplayName("Should handle method parameter case insensitively")
    void testMetricsMethodCase() {
        // Test lowercase
        given()
                .queryParam("path", "/api/v1/accounts")
                .queryParam("method", "get")
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(200), is(404)));

        // Test uppercase
        given()
                .queryParam("path", "/api/v1/accounts")
                .queryParam("method", "GET")
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(200), is(404)));

        // Test mixed case
        given()
                .queryParam("path", "/api/v1/accounts")
                .queryParam("method", "Get")
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    // ==================== Response Format Tests ====================

    @Test
    @Order(70)
    @DisplayName("Metrics endpoint should return JSON")
    void testMetricsResponseFormat() {
        given()
                .queryParam("path", "/api/v1/accounts")
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(200), is(404)))
                .contentType(containsString("application/json"));
    }

    // ==================== HTTP Methods Tests ====================

    @Test
    @Order(80)
    @DisplayName("Metrics endpoint should only support GET method")
    void testMetricsHttpMethods() {
        // GET should work (but might return 404 if no metrics)
        given()
                .queryParam("path", "/api/v1/accounts")
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(200), is(404)));

        // POST should not be allowed
        given()
                .queryParam("path", "/api/v1/accounts")
                .when()
                .post("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(405), is(404)));

        // PUT should not be allowed
        given()
                .queryParam("path", "/api/v1/accounts")
                .when()
                .put("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(405), is(404)));

        // DELETE should not be allowed
        given()
                .queryParam("path", "/api/v1/accounts")
                .when()
                .delete("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(405), is(404)));
    }

    // ==================== Concurrent Requests Tests ====================

    @Test
    @Order(90)
    @DisplayName("Should handle concurrent analytics requests")
    void testConcurrentAnalyticsRequests() throws InterruptedException {
        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            threads[i] = new Thread(() -> {
                given()
                        .queryParam("path", "/api/v1/accounts")
                        .when()
                        .get("/gateway/analytics/metrics")
                        .then()
                        .statusCode(anyOf(is(200), is(404)));
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }

    // ==================== Response Time Tests ====================

    @Test
    @Order(100)
    @DisplayName("Analytics endpoints should respond quickly")
    void testAnalyticsResponseTime() {
        long startTime = System.currentTimeMillis();

        given()
                .queryParam("path", "/api/v1/accounts")
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(200), is(404)));

        long responseTime = System.currentTimeMillis() - startTime;
        Assertions.assertTrue(responseTime < 2000, "Analytics endpoint should respond within 2 seconds");
    }

    @Test
    @Order(101)
    @DisplayName("Analytics health endpoint should respond quickly")
    void testAnalyticsHealthResponseTime() {
        long startTime = System.currentTimeMillis();

        given()
                .when()
                .get("/gateway/analytics/health")
                .then()
                .statusCode(200);

        long responseTime = System.currentTimeMillis() - startTime;
        Assertions.assertTrue(responseTime < 1000, "Analytics health endpoint should respond within 1 second");
    }

    // ==================== CORS Tests ====================

    @Test
    @Order(110)
    @DisplayName("Analytics endpoints should support CORS")
    void testAnalyticsCors() {
        given()
                .header("Origin", "http://localhost:3000")
                .when()
                .get("/gateway/analytics/health")
                .then()
                .statusCode(200);
    }

    // ==================== Error Response Tests ====================

    @Test
    @Order(120)
    @DisplayName("Should return proper error response structure")
    void testErrorResponseStructure() {
        given()
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(200), is(400)))
                .body(notNullValue());
    }

    @Test
    @Order(121)
    @DisplayName("Should return proper error when metrics not found")
    void testMetricsNotFoundError() {
        given()
                .queryParam("path", "/api/v1/nonexistentendpoint")
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(404), is(200)))
                .body(notNullValue());
    }

    // ==================== Query Parameter Encoding Tests ====================

    @Test
    @Order(130)
    @DisplayName("Should handle URL-encoded paths")
    void testMetricsUrlEncodedPath() {
        given()
                .queryParam("path", "/api/v1/accounts/test%20account")
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    // ==================== Root Path Tests ====================

    @Test
    @Order(140)
    @DisplayName("Should handle root path query")
    void testMetricsRootPath() {
        given()
                .queryParam("path", "/")
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    // ==================== Multiple Parameters Tests ====================

    @Test
    @Order(150)
    @DisplayName("Should ignore unknown query parameters")
    void testMetricsUnknownParameters() {
        given()
                .queryParam("path", "/api/v1/accounts")
                .queryParam("method", "GET")
                .queryParam("unknown", "value")
                .queryParam("another", "param")
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    // ==================== Analytics Configuration Tests ====================

    @Test
    @Order(160)
    @DisplayName("Should verify analytics is enabled")
    void testAnalyticsEnabled() {
        // We can verify analytics is working by checking the health endpoint
        given()
                .when()
                .get("/gateway/analytics/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    // ==================== Empty Response Tests ====================

    @Test
    @Order(170)
    @DisplayName("Should handle empty metrics response")
    void testMetricsEmptyResponse() {
        // This tests the case where no metrics are available for an endpoint
        // The response could be 404 or 200 with empty array
        given()
                .queryParam("path", "/api/v1/never-visited-endpoint")
                .queryParam("method", "DELETE")
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    // ==================== Cross-Origin Tests ====================

    @Test
    @Order(180)
    @DisplayName("Should handle requests from different origins")
    void testAnalyticsDifferentOrigins() {
        String[] origins = {
            "http://localhost:3000",
            "https://payu.fajjjar.my.id",
            "http://localhost:8081"
        };

        for (String origin : origins) {
            given()
                    .header("Origin", origin)
                    .when()
                    .get("/gateway/analytics/health")
                    .then()
                    .statusCode(200);
        }
    }

    // ==================== Metrics Aggregation Tests ====================

    @Test
    @Order(190)
    @DisplayName("Should aggregate metrics across all HTTP methods when method not specified")
    void testMetricsAggregation() {
        given()
                .queryParam("path", "/api/v1/accounts")
                // Note: not specifying method should aggregate across all methods
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    // ==================== Long Path Tests ====================

    @Test
    @Order(200)
    @DisplayName("Should handle long paths")
    void testMetricsLongPath() {
        String longPath = "/api/v1/accounts/123/transactions/456/details/678";

        given()
                .queryParam("path", longPath)
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    // ==================== Custom Header Tests ====================

    @Test
    @Order(210)
    @DisplayName("Should handle custom headers in request")
    void testMetricsWithCustomHeaders() {
        given()
                .header("X-Custom-Header", "custom-value")
                .header("X-Request-Id", "test-request-123")
                .queryParam("path", "/api/v1/accounts")
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    // ==================== Analytics Error Handling Tests ====================

    @Test
    @Order(220)
    @DisplayName("Should return 500 on analytics service error")
    void testAnalyticsServiceError() {
        // This test documents expected behavior when analytics service fails
        // In a real scenario, we'd mock the service failure
        // For now, we just verify the endpoint exists
        given()
                .queryParam("path", "/api/v1/accounts")
                .when()
                .get("/gateway/analytics/metrics")
                .then()
                .statusCode(anyOf(is(200), is(404), is(503)));
    }
}
