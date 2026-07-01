package id.payu.gateway.adapter.filter;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests to verify that financial endpoints strictly reject requests
 * when the Idempotency-Key or X-Idempotency-Key header is missing.
 */
@QuarkusTest
@TestProfile(IdempotencyFilterEnforcedTestProfile.class)
@DisplayName("Idempotency Filter Enforcement Tests")
public class IdempotencyFilterEnforcedTest {

    private static final String STANDARD_HEADER = "Idempotency-Key";

    @Test
    @DisplayName("Should accept request without idempotency key for non-financial path")
    public void testRequestWithoutIdempotencyKeyNonFinancial() {
        given()
            .contentType("application/json")
            .when()
            .post("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(200), is(201), is(202), is(404), is(429), is(503)));
    }

    @Test
    @DisplayName("Should reject request without idempotency key for financial path: /api/v1/disbursements")
    public void testRejectRequestWithoutIdempotencyKeyForFinancialPath() {
        given()
            .contentType("application/json")
            .when()
            .post("/api/v1/disbursements")
            .then()
            .statusCode(400)
            .body("error", is("IDEMPOTENCY_KEY_REQUIRED"))
            .body("code", is("GAT_IDM_001"));
    }

    @Test
    @DisplayName("Should reject request without idempotency key for SNAP-BI path: /api/v1/v1/partner/payments")
    public void testRejectRequestWithoutIdempotencyKeyForSnapBiPath() {
        given()
            .contentType("application/json")
            .when()
            .post("/api/v1/v1/partner/payments")
            .then()
            .statusCode(400)
            .body("error", is("IDEMPOTENCY_KEY_REQUIRED"))
            .body("code", is("GAT_IDM_001"));
    }

    @Test
    @DisplayName("Should accept request with idempotency key for financial path: /api/v1/disbursements")
    public void testAcceptRequestWithIdempotencyKeyForFinancialPath() {
        String key = "test-fin-key-" + System.currentTimeMillis();
        given()
            .header(STANDARD_HEADER, key)
            .contentType("application/json")
            .when()
            .post("/api/v1/disbursements")
            .then()
            .statusCode(anyOf(is(200), is(201), is(202), is(404), is(429), is(503)));
    }
}
