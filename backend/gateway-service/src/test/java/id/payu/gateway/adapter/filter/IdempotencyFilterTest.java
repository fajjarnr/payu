package id.payu.gateway.adapter.filter;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

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
 * @see IdempotencyFilter
 */
@QuarkusTest
@Disabled("Idempotency is disabled in tests - requires backend services running on port 8081")
@DisplayName("Idempotency Filter Tests")
public class IdempotencyFilterTest {

    private static final String IDEMPOTENCY_KEY = "test-idempotency-key-" + System.currentTimeMillis();

    // Standard header name as per RFC 7239 and industry best practices
    private static final String STANDARD_HEADER = "Idempotency-Key";
    // Legacy header name for backward compatibility
    private static final String LEGACY_HEADER = "X-Idempotency-Key";

    @Test
    @DisplayName("Should accept request with standard Idempotency-Key header")
    public void testRequestWithStandardIdempotencyKey() {
        given()
            .header(STANDARD_HEADER, IDEMPOTENCY_KEY)
            .contentType("application/json")
            .when()
            .post("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(200), is(201), is(202)));
    }

    @Test
    @DisplayName("Should accept request with legacy X-Idempotency-Key header (backward compatibility)")
    public void testRequestWithLegacyIdempotencyKey() {
        given()
            .header(LEGACY_HEADER, IDEMPOTENCY_KEY + "-legacy")
            .contentType("application/json")
            .when()
            .post("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(200), is(201), is(202)));
    }

    @Test
    @DisplayName("Should prioritize standard header over legacy header when both are present")
    public void testStandardHeaderTakesPrecedence() {
        String standardKey = "standard-key-" + System.currentTimeMillis();
        String legacyKey = "legacy-key-" + System.currentTimeMillis();

        // First request with standard header
        given()
            .header(STANDARD_HEADER, standardKey)
            .header(LEGACY_HEADER, legacyKey) // Should be ignored
            .contentType("application/json")
            .when()
            .post("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(200), is(201), is(202)));

        // Second request with same standard key - should return cached response
        given()
            .header(STANDARD_HEADER, standardKey)
            .header(LEGACY_HEADER, legacyKey) // Should be ignored
            .contentType("application/json")
            .when()
            .post("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(200), is(201), is(202)))
            .header("Idempotency-Replayed", "true")
            .header("X-Idempotency-Replayed", "true");
    }

    @Test
    @DisplayName("Should return cached response for duplicate idempotency key (standard header)")
    public void testDuplicateIdempotencyKeyWithStandardHeader() {
        String idempotencyKey = "duplicate-test-standard-" + System.currentTimeMillis();

        // First request
        given()
            .header(STANDARD_HEADER, idempotencyKey)
            .contentType("application/json")
            .when()
            .post("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(200), is(201), is(202)));

        // Second request with same key
        given()
            .header(STANDARD_HEADER, idempotencyKey)
            .contentType("application/json")
            .when()
            .post("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(200), is(201), is(202)))
            .header("Idempotency-Replayed", "true")
            .header("X-Idempotency-Replayed", "true");
    }

    @Test
    @DisplayName("Should return cached response for duplicate idempotency key (legacy header)")
    public void testDuplicateIdempotencyKeyWithLegacyHeader() {
        String idempotencyKey = "duplicate-test-legacy-" + System.currentTimeMillis();

        // First request
        given()
            .header(LEGACY_HEADER, idempotencyKey)
            .contentType("application/json")
            .when()
            .post("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(200), is(201), is(202)));

        // Second request with same key
        given()
            .header(LEGACY_HEADER, idempotencyKey)
            .contentType("application/json")
            .when()
            .post("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(200), is(201), is(202)))
            .header("Idempotency-Replayed", "true")
            .header("X-Idempotency-Replayed", "true");
    }

    @Test
    @DisplayName("Should allow request without idempotency key")
    public void testRequestWithoutIdempotencyKey() {
        given()
            .contentType("application/json")
            .when()
            .post("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(200), is(201), is(202)));
    }

    @Test
    @DisplayName("Should not apply idempotency to GET requests with standard header")
    public void testGetRequestSkippedWithStandardHeader() {
        given()
            .header(STANDARD_HEADER, IDEMPOTENCY_KEY)
            .when()
            .get("/api/v1/accounts")
            .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("Should not apply idempotency to GET requests with legacy header")
    public void testGetRequestSkippedWithLegacyHeader() {
        given()
            .header(LEGACY_HEADER, IDEMPOTENCY_KEY)
            .when()
            .get("/api/v1/accounts")
            .then()
            .statusCode(200);
    }
}
