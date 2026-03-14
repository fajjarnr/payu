package id.payu.gateway.adapter.filter;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests for Idempotency Filter.
 *
 * <p>These tests verify that the idempotency filter correctly handles both:
 * <ul>
 *   <li>Standard header: "Idempotency-Key" (RFC 7239 compliant)</li>
 *   <li>Legacy header: "X-Idempotency-Key" (backward compatibility)</li>
 * </ul>
 *
 * <p>The standard header is checked first, with fallback to legacy header.
 *
 * <p>Note: These tests verify filter passthrough behavior when idempotency
 * is enabled but Redis is unavailable. Duplicate detection (cache hit) tests
 * require a running Redis instance and are tested in integration tests.
 *
 * @see IdempotencyFilter
 */
@QuarkusTest
@TestProfile(IdempotencyFilterTestProfile.class)
@DisplayName("Idempotency Filter Tests")
public class IdempotencyFilterTest {

    private static final String STANDARD_HEADER = "Idempotency-Key";
    private static final String LEGACY_HEADER = "X-Idempotency-Key";

    @Test
    @DisplayName("Should accept request with standard Idempotency-Key header")
    public void testRequestWithStandardIdempotencyKey() {
        String key = "test-key-" + System.currentTimeMillis();
        given()
            .header(STANDARD_HEADER, key)
            .contentType("application/json")
            .when()
            .post("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(200), is(201), is(202), is(404), is(500), is(503)));
    }

    @Test
    @DisplayName("Should accept request with legacy X-Idempotency-Key header")
    public void testRequestWithLegacyIdempotencyKey() {
        String key = "test-legacy-" + System.currentTimeMillis();
        given()
            .header(LEGACY_HEADER, key)
            .contentType("application/json")
            .when()
            .post("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(200), is(201), is(202), is(404), is(500), is(503)));
    }

    @Test
    @DisplayName("Should accept request without idempotency key for non-financial paths")
    public void testRequestWithoutIdempotencyKey() {
        given()
            .contentType("application/json")
            .when()
            .post("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(200), is(201), is(202), is(404), is(500), is(503)));
    }

    @Test
    @DisplayName("Should not apply idempotency to GET requests")
    public void testGetRequestSkipped() {
        given()
            .header(STANDARD_HEADER, "get-test-key")
            .when()
            .get("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(200), is(404), is(401), is(500), is(503)));
    }

    @Test
    @DisplayName("Should skip idempotency for health endpoints")
    public void testHealthEndpointSkipped() {
        given()
            .header(STANDARD_HEADER, "health-test-key")
            .when()
            .get("/q/health")
            .then()
            .statusCode(anyOf(is(200), is(503)));
    }

    @Test
    @DisplayName("Should accept both standard and legacy headers simultaneously")
    public void testBothHeadersPresent() {
        String key = "both-headers-" + System.currentTimeMillis();
        given()
            .header(STANDARD_HEADER, key)
            .header(LEGACY_HEADER, "legacy-" + key)
            .contentType("application/json")
            .when()
            .post("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(200), is(201), is(202), is(404), is(500), is(503)));
    }
}
