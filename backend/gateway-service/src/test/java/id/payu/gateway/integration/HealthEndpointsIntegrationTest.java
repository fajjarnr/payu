package id.payu.gateway.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Gateway Health Endpoints.
 *
 * <p>Tests all health-related endpoints including:
 * <ul>
 *   <li>Custom health endpoint (/health)</li>
 *   <li>Status endpoint (/status)</li>
 *   <li>Version endpoint (/version)</li>
 *   <li>Quarkus health endpoints (/q/health, /q/health/ready, /q/health/live)</li>
 * </ul>
 *
 * @author PayU Engineering Team
 * @since 1.0.0
 */
@QuarkusTest
@DisplayName("Health Endpoints Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HealthEndpointsIntegrationTest {

    @BeforeEach
    void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    // ==================== Custom Health Endpoint Tests ====================

    @Test
    @Order(1)
    @DisplayName("Custom health endpoint should return UP status")
    void testCustomHealthEndpoint() {
        given()
                .when()
                .get("/health")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("status", equalTo("UP"))
                .body("service", equalTo("gateway-service"))
                .body("version", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    @Order(2)
    @DisplayName("Custom health endpoint should include service metadata")
    void testCustomHealthMetadata() {
        given()
                .when()
                .get("/health")
                .then()
                .statusCode(200)
                .body("service", equalTo("gateway-service"))
                .body("version", equalTo("1.0.0"));
    }

    // ==================== Status Endpoint Tests ====================

    @Test
    @Order(10)
    @DisplayName("Status endpoint should return detailed system information")
    void testStatusEndpoint() {
        given()
                .when()
                .get("/status")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("status", equalTo("UP"))
                .body("service", equalTo("gateway-service"))
                .body("uptime", notNullValue())
                .body("uptimeMs", greaterThan(0L))
                .body("startTime", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    @Order(11)
    @DisplayName("Status endpoint should include memory information")
    void testStatusMemoryInfo() {
        given()
                .when()
                .get("/status")
                .then()
                .statusCode(200)
                .body("memory", notNullValue())
                .body("memory.total", greaterThan(0L))
                .body("memory.free", greaterThanOrEqualTo(0L))
                .body("memory.used", greaterThanOrEqualTo(0L))
                .body("memory.max", greaterThan(0L));
    }

    @Test
    @Order(12)
    @DisplayName("Status endpoint should include processor count")
    void testStatusProcessors() {
        given()
                .when()
                .get("/status")
                .then()
                .statusCode(200)
                .body("processors", greaterThan(0));
    }

    @Test
    @Order(13)
    @DisplayName("Status endpoint should include formatted uptime string")
    void testStatusUptimeFormatted() {
        given()
                .when()
                .get("/status")
                .then()
                .statusCode(200)
                .body("uptime", notNullValue())
                .body("uptime", matchesPattern("^(\\d+d )?\\d+h \\d+m \\d+s$|^\\d+m \\d+s$|^\\d+s$"));
    }

    // ==================== Version Endpoint Tests ====================

    @Test
    @Order(20)
    @DisplayName("Version endpoint should return version information")
    void testVersionEndpoint() {
        given()
                .when()
                .get("/version")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("service", equalTo("gateway-service"))
                .body("version", equalTo("1.0.0"))
                .body("apiVersion", equalTo("v1"))
                .body("buildTime", notNullValue());
    }

    // ==================== Quarkus Health Endpoint Tests ====================

    @Test
    @Order(30)
    @DisplayName("Quarkus health check should return UP")
    void testQuarkusHealth() {
        given()
                .when()
                .get("/q/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    @Order(31)
    @DisplayName("Quarkus health readiness should pass")
    void testQuarkusHealthReadiness() {
        given()
                .when()
                .get("/q/health/ready")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    @Order(32)
    @DisplayName("Quarkus health liveness should pass")
    void testQuarkusHealthLiveness() {
        given()
                .when()
                .get("/q/health/live")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    @Order(33)
    @DisplayName("Quarkus health should include health checks")
    void testQuarkusHealthChecks() {
        given()
                .when()
                .get("/q/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("checks", notNullValue());
    }

    // ==================== Analytics Health Tests ====================

    @Test
    @Order(40)
    @DisplayName("Analytics health endpoint should return UP")
    void testAnalyticsHealthEndpoint() {
        given()
                .when()
                .get("/gateway/analytics/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("service", equalTo("analytics"));
    }

    // ==================== Concurrent Health Check Tests ====================

    @Test
    @Order(50)
    @DisplayName("Should handle concurrent health check requests")
    void testConcurrentHealthChecks() throws InterruptedException {
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                given()
                        .when()
                        .get("/health")
                        .then()
                        .statusCode(200)
                        .body("status", equalTo("UP"));
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }

    // ==================== Response Time Tests ====================

    @Test
    @Order(60)
    @DisplayName("Health endpoints should respond quickly")
    void testHealthEndpointResponseTime() {
        long startTime = System.currentTimeMillis();

        given()
                .when()
                .get("/health")
                .then()
                .statusCode(200);

        long responseTime = System.currentTimeMillis() - startTime;
        Assertions.assertTrue(responseTime < 1000, "Health endpoint should respond within 1 second");
    }

    @Test
    @Order(61)
    @DisplayName("Status endpoint should respond quickly")
    void testStatusEndpointResponseTime() {
        long startTime = System.currentTimeMillis();

        given()
                .when()
                .get("/status")
                .then()
                .statusCode(200);

        long responseTime = System.currentTimeMillis() - startTime;
        Assertions.assertTrue(responseTime < 1000, "Status endpoint should respond within 1 second");
    }

    // ==================== Different HTTP Methods Tests ====================

    @Test
    @Order(70)
    @DisplayName("Health endpoints should only support GET method")
    void testHealthEndpointMethods() {
        // GET should work
        given()
                .when()
                .get("/health")
                .then()
                .statusCode(200);

        // POST should not be allowed (might return 405 or 404 depending on routing)
        given()
                .when()
                .post("/health")
                .then()
                .statusCode(anyOf(is(405), is(404)));

        // PUT should not be allowed
        given()
                .when()
                .put("/health")
                .then()
                .statusCode(anyOf(is(405), is(404)));

        // DELETE should not be allowed
        given()
                .when()
                .delete("/health")
                .then()
                .statusCode(anyOf(is(405), is(404)));
    }

    // ==================== JSON Format Tests ====================

    @Test
    @Order(80)
    @DisplayName("Health endpoints should return valid JSON")
    void testHealthEndpointJsonFormat() {
        given()
                .when()
                .get("/health")
                .then()
                .statusCode(200)
                .contentType(containsString("application/json"));
    }

    @Test
    @Order(81)
    @DisplayName("Status endpoint should return valid JSON")
    void testStatusEndpointJsonFormat() {
        given()
                .when()
                .get("/status")
                .then()
                .statusCode(200)
                .contentType(containsString("application/json"));
    }

    // ==================== Cache Control Tests ====================

    @Test
    @Order(90)
    @DisplayName("Health endpoints should not be cached")
    void testHealthEndpointNoCache() {
        given()
                .when()
                .get("/health")
                .then()
                .statusCode(200)
                .header("Cache-Control", containsString("no-cache"));
    }

    // ==================== Uptime Tracking Tests ====================

    @Test
    @Order(100)
    @DisplayName("Status endpoint uptime should increase over time")
    void testUptimeIncreases() throws InterruptedException {
        // Get initial uptime
        long initialUptime = given()
                .when()
                .get("/status")
                .then()
                .extract()
                .path("uptimeMs");

        // Wait a bit
        Thread.sleep(100);

        // Get uptime again
        long laterUptime = given()
                .when()
                .get("/status")
                .then()
                .extract()
                .path("uptimeMs");

        Assertions.assertTrue(laterUptime > initialUptime, "Uptime should increase over time");
    }

    // ==================== Error Handling Tests ====================

    @Test
    @Order(110)
    @DisplayName("Should handle non-existent health endpoints gracefully")
    void testNonExistentHealthEndpoint() {
        given()
                .when()
                .get("/health/nonexistent")
                .then()
                .statusCode(anyOf(is(404), is(405)));
    }

    // ==================== Memory Validation Tests ====================

    @Test
    @Order(120)
    @DisplayName("Status endpoint memory values should be consistent")
    void testMemoryConsistency() {
        long total = given()
                .when()
                .get("/status")
                .then()
                .extract()
                .path("memory.total");

        long used = given()
                .when()
                .get("/status")
                .then()
                .extract()
                .path("memory.used");

        long free = given()
                .when()
                .get("/status")
                .then()
                .extract()
                .path("memory.free");

        // Used + Free should be approximately equal to Total
        Assertions.assertTrue(used + free <= total, "Used + Free should not exceed Total");
    }

    // ==================== Service Identification Tests ====================

    @Test
    @Order(130)
    @DisplayName("All health endpoints should identify as gateway-service")
    void testServiceIdentification() {
        // Custom health endpoint
        given()
                .when()
                .get("/health")
                .then()
                .body("service", equalTo("gateway-service"));

        // Status endpoint
        given()
                .when()
                .get("/status")
                .then()
                .body("service", equalTo("gateway-service"));

        // Version endpoint
        given()
                .when()
                .get("/version")
                .then()
                .body("service", equalTo("gateway-service"));
    }

    // ==================== Timestamp Tests ====================

    @Test
    @Order(140)
    @DisplayName("Status endpoint timestamp should be current")
    void testStatusTimestampCurrent() {
        long beforeRequest = System.currentTimeMillis();

        long timestamp = given()
                .when()
                .get("/status")
                .then()
                .extract()
                .path("timestamp");

        long afterRequest = System.currentTimeMillis();

        // Timestamp should be between before and after request times
        // Allow for some clock skew
        Assertions.assertTrue(timestamp >= beforeRequest - 1000);
        Assertions.assertTrue(timestamp <= afterRequest + 1000);
    }

    @Test
    @Order(141)
    @DisplayName("Health endpoint timestamp should be current")
    void testHealthTimestampCurrent() {
        long beforeRequest = System.currentTimeMillis();

        long timestamp = given()
                .when()
                .get("/health")
                .then()
                .extract()
                .path("timestamp");

        long afterRequest = System.currentTimeMillis();

        Assertions.assertTrue(timestamp >= beforeRequest - 1000);
        Assertions.assertTrue(timestamp <= afterRequest + 1000);
    }

    // ==================== CORS Tests ====================

    @Test
    @Order(150)
    @DisplayName("Health endpoints should support CORS preflight")
    void testHealthEndpointCors() {
        given()
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .when()
                .options("/health")
                .then()
                .statusCode(anyOf(is(204), is(200)));
    }

    // ==================== Cross-Service Health Tests ====================

    @Test
    @Order(160)
    @DisplayName("All health endpoints should be accessible")
    void testAllHealthEndpointsAccessible() {
        // List of all health-related endpoints
        String[] endpoints = {
            "/health",
            "/status",
            "/version",
            "/q/health",
            "/q/health/ready",
            "/q/health/live",
            "/gateway/analytics/health"
        };

        for (String endpoint : endpoints) {
            given()
                    .when()
                    .get(endpoint)
                    .then()
                    .statusCode(200);
        }
    }

    // ==================== Content Type Tests ====================

    @Test
    @Order(170)
    @DisplayName("Health endpoints should return UTF-8 charset")
    void testHealthEndpointCharset() {
        given()
                .when()
                .get("/health")
                .then()
                .contentType(matchesPattern(".*charset=utf-8.*"));
    }

    // ==================== Start Time Tests ====================

    @Test
    @Order(180)
    @DisplayName("Status endpoint start time should be in the past")
    void testStartTimeIsPast() {
        long startTime = given()
                .when()
                .get("/status")
                .then()
                .extract()
                .path("startTime");

        long currentTime = System.currentTimeMillis();

        Assertions.assertTrue(startTime < currentTime, "Start time should be in the past");
        Assertions.assertTrue(startTime > currentTime - 3600000, "Start time should be within the last hour");
    }
}
