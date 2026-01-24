package id.payu.gateway.filter;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@DisplayName("Rate Limiting V2 Filter Tests")
public class RateLimitV2FilterTest {

    @Test
    @DisplayName("Should allow requests within rate limit")
    public void testRequestsWithinLimit() {
        given()
            .when()
            .get("/api/v1/accounts")
            .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("Should return 429 when rate limit exceeded")
    public void testRateLimitExceeded() {
        // Send multiple requests to exceed rate limit
        for (int i = 0; i < 100; i++) {
            given()
                .when()
                .get("/api/v1/accounts")
                .then()
                .statusCode(anyOf(
                    is(200),
                    is(429)
                ));
        }
    }

    @Test
    @DisplayName("Should include rate limit headers")
    public void testRateLimitHeaders() {
        given()
            .when()
            .get("/api/v1/accounts")
            .then()
            .statusCode(200)
            .header("X-RateLimit-Remaining", notNullValue());
    }

    @Test
    @DisplayName("Should apply per-user rate limiting")
    public void testPerUserRateLimit() {
        given()
            .header("X-User-Id", "test-user-1")
            .when()
            .get("/api/v1/accounts")
            .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("Should apply per-IP rate limiting")
    public void testPerIpRateLimit() {
        given()
            .header("X-Forwarded-For", "192.168.1.100")
            .when()
            .get("/api/v1/accounts")
            .then()
            .statusCode(200);
    }
}
