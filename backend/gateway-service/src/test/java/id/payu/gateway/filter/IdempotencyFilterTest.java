package id.payu.gateway.filter;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@Disabled("Idempotency is disabled in tests - requires backend services running on port 8081")
@DisplayName("Idempotency Filter Tests")
public class IdempotencyFilterTest {

    private static final String IDEMPOTENCY_KEY = "test-idempotency-key-" + System.currentTimeMillis();

    @Test
    @DisplayName("Should accept request with idempotency key")
    public void testRequestWithIdempotencyKey() {
        given()
            .header("X-Idempotency-Key", IDEMPOTENCY_KEY)
            .contentType("application/json")
            .when()
            .post("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(200), is(201), is(202)));
    }

    @Test
    @DisplayName("Should return cached response for duplicate idempotency key")
    public void testDuplicateIdempotencyKey() {
        String idempotencyKey = "duplicate-test-" + System.currentTimeMillis();

        // First request
        given()
            .header("X-Idempotency-Key", idempotencyKey)
            .contentType("application/json")
            .when()
            .post("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(200), is(201), is(202)));

        // Second request with same key
        given()
            .header("X-Idempotency-Key", idempotencyKey)
            .contentType("application/json")
            .when()
            .post("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(200), is(201), is(202)))
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
    @DisplayName("Should not apply idempotency to GET requests")
    public void testGetRequestSkipped() {
        given()
            .header("X-Idempotency-Key", IDEMPOTENCY_KEY)
            .when()
            .get("/api/v1/accounts")
            .then()
            .statusCode(200);
    }
}
