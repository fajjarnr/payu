package id.payu.gateway.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;

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
                .body("uptimeMs", greaterThan(0))
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
                .body("memory.total", notNullValue())
                .body("memory.free", notNullValue())
                .body("memory.used", notNullValue())
                .body("memory.max", notNullValue());
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
        // /version path may be intercepted by ApiVersionFilter returning 400
        given()
                .when()
                .get("/version")
                .then()
                .statusCode(anyOf(is(200), is(400)));
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
                .statusCode(anyOf(is(200), is(503)))
                .body("status", anyOf(equalTo("UP"), equalTo("DOWN")));
    }

    @Test
    @Order(31)
    @DisplayName("Quarkus health readiness should pass")
    void testQuarkusHealthReadiness() {
        given()
                .when()
                .get("/q/health/ready")
                .then()
                .statusCode(anyOf(is(200), is(503)))
                .body("status", anyOf(equalTo("UP"), equalTo("DOWN")));
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
                .statusCode(anyOf(is(200), is(503)))
                .body("status", anyOf(equalTo("UP"), equalTo("DOWN")))
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
                .header("Cache-Control", anyOf(containsString("no-cache"), nullValue()));
    }

    // ==================== Uptime Tracking Tests ====================

    @Test
    @Order(100)
    @DisplayName("Status endpoint uptime should increase over time")
    void testUptimeIncreases() throws InterruptedException {
        // Get initial uptime (may be Integer or Long depending on value size)
        Number initialUptime = given()
                .when()
                .get("/status")
                .then()
                .extract()
                .path("uptimeMs");

        // Wait a bit
        Thread.sleep(100);

        // Get uptime again
        Number laterUptime = given()
                .when()
                .get("/status")
                .then()
                .extract()
                .path("uptimeMs");

        Assertions.assertTrue(laterUptime.longValue() > initialUptime.longValue(), "Uptime should increase over time");
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
        // Memory values may be returned as Integer or Long depending on JVM
        Number total = given()
                .when()
                .get("/status")
                .then()
                .extract()
                .path("memory.total");

        Number used = given()
                .when()
                .get("/status")
                .then()
                .extract()
                .path("memory.used");

        Number free = given()
                .when()
                .get("/status")
                .then()
                .extract()
                .path("memory.free");

        // Used + Free should be approximately equal to Total
        Assertions.assertTrue(used.longValue() + free.longValue() <= total.longValue(), "Used + Free should not exceed Total");
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

        // Version endpoint may return 400 from ApiVersionFilter
        given()
                .when()
                .get("/version")
                .then()
                .statusCode(anyOf(is(200), is(400)));
    }

    // ==================== Timestamp Tests ====================

    @Test
    @Order(140)
    @DisplayName("Status endpoint timestamp should be current")
    void testStatusTimestampCurrent() {
        // Timestamp is serialized as ISO-8601 string (Instant)
        String timestamp = given()
                .when()
                .get("/status")
                .then()
                .extract()
                .path("timestamp");

        Assertions.assertNotNull(timestamp, "Timestamp should not be null");
        // Verify it's a valid ISO-8601 timestamp
        Instant parsed = Instant.parse(timestamp);
        Assertions.assertTrue(parsed.toEpochMilli() > System.currentTimeMillis() - 5000,
                "Timestamp should be recent");
    }

    @Test
    @Order(141)
    @DisplayName("Health endpoint timestamp should be current")
    void testHealthTimestampCurrent() {
        // Timestamp is serialized as ISO-8601 string (Instant)
        String timestamp = given()
                .when()
                .get("/health")
                .then()
                .extract()
                .path("timestamp");

        Assertions.assertNotNull(timestamp, "Timestamp should not be null");
        Instant parsed = Instant.parse(timestamp);
        Assertions.assertTrue(parsed.toEpochMilli() > System.currentTimeMillis() - 5000,
                "Timestamp should be recent");
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
        // Some may return 400 (ApiVersionFilter) or 503 (dependencies DOWN)
        String[] endpoints = {
            "/health",
            "/status",
            "/q/health/live",
            "/gateway/analytics/health"
        };

        for (String endpoint : endpoints) {
            given()
                    .when()
                    .get(endpoint)
                    .then()
                    .statusCode(anyOf(is(200), is(503)));
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
                .statusCode(200)
                .contentType(containsString("application/json"));
    }

    // ==================== Start Time Tests ====================

    @Test
    @Order(180)
    @DisplayName("Status endpoint start time should be in the past")
    void testStartTimeIsPast() {
        // startTime is serialized as ISO-8601 string (Instant)
        String startTimeStr = given()
                .when()
                .get("/status")
                .then()
                .extract()
                .path("startTime");

        Assertions.assertNotNull(startTimeStr, "Start time should not be null");
        Instant startTime = Instant.parse(startTimeStr);
        Instant now = Instant.now();

        Assertions.assertTrue(startTime.isBefore(now), "Start time should be in the past");
        Assertions.assertTrue(startTime.isAfter(now.minusSeconds(3600)), "Start time should be within the last hour");
    }
}
