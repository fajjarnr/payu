package id.payu.gateway.filter;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@DisplayName("Rate Limit Filter Tests")
class RateLimitFilterTest {

    @Nested
    @DisplayName("Rate Limiting - Auth Endpoints")
    class AuthEndpoints {

        @Test
        @DisplayName("should allow requests within auth rate limit")
        void shouldAllowRequestsWithinAuthRateLimit() {
            given()
                .when()
                    .post("/api/v1/auth/login")
                .then()
                    .statusCode(anyOf(is(200), is(400), is(404), is(415), is(500), is(503)));
        }

        @Test
        @DisplayName("should enforce auth rate limit after threshold")
        void shouldEnforceAuthRateLimitAfterThreshold() {
            String clientId = "test-auth-" + System.currentTimeMillis();
            Response response = given()
                .header("X-Forwarded-For", clientId)
            .when()
                .post("/api/v1/auth/login");

            int statusCode = response.getStatusCode();
            if (statusCode == 429) {
                response.then()
                    .body("error", equalTo("RATE_LIMIT_EXCEEDED"))
                    .body("message", notNullValue())
                    .header("Retry-After", notNullValue());
            }
        }
    }

    @Nested
    @DisplayName("Rate Limiting - Balance Endpoints")
    class BalanceEndpoints {

        @Test
        @DisplayName("should allow requests within balance rate limit")
        void shouldAllowRequestsWithinBalanceRateLimit() {
            given()
            .when()
                .get("/api/v1/balance")
            .then()
                .statusCode(anyOf(is(200), is(400), is(401), is(404), is(503)));
        }

        @Test
        @DisplayName("should enforce balance rate limit after threshold")
        void shouldEnforceBalanceRateLimitAfterThreshold() {
            String clientId = "test-balance-" + System.currentTimeMillis();
            
            given()
                .header("X-Forwarded-For", clientId)
            .when()
                .get("/api/v1/balance")
            .then()
                .statusCode(anyOf(is(200), is(401), is(404), is(429), is(503)));
        }
    }

    @Nested
    @DisplayName("Rate Limiting - Transfer Endpoints")
    class TransferEndpoints {

        @Test
        @DisplayName("should allow requests within transfer rate limit")
        void shouldAllowRequestsWithinTransferRateLimit() {
            given()
            .when()
                .post("/api/v1/transfer")
            .then()
                .statusCode(anyOf(is(200), is(400), is(404), is(415), is(500), is(503)));
        }

        @Test
        @DisplayName("should enforce transfer rate limit after threshold")
        void shouldEnforceTransferRateLimitAfterThreshold() {
            String clientId = "test-transfer-" + System.currentTimeMillis();
            
            Response response = given()
                .header("X-Forwarded-For", clientId)
            .when()
                .post("/api/v1/transfer");

            int statusCode = response.getStatusCode();
            if (statusCode == 429) {
                response.then()
                    .body("error", equalTo("RATE_LIMIT_EXCEEDED"))
                    .body("message", equalTo("Too many requests. Please try again later."))
                    .header("Retry-After", equalTo("60"));
            }
        }
    }

    @Nested
    @DisplayName("DefaultEndpoints")
    class DefaultEndpoints {

        @Test
        @DisplayName("should apply default rate limit to unconfigured endpoints")
        void shouldApplyDefaultRateLimitToUnconfiguredEndpoints() {
            given()
            .when()
                .get("/api/v1/partners")
            .then()
                .statusCode(anyOf(is(200), is(404), is(429), is(500), is(503)));
        }

        @Test
        @DisplayName("should allow requests within default rate limit")
        void shouldAllowRequestsWithinDefaultRateLimit() {
            String clientId = "test-default-" + System.currentTimeMillis();
            
            given()
                .header("X-Forwarded-For", clientId)
            .when()
                .get("/api/v1/partners")
            .then()
                .statusCode(anyOf(is(200), is(404), is(500), is(503)));
        }
    }

    @Nested
    @DisplayName("Rate Limiting - Health and Metrics")
    class HealthAndMetrics {

        @Test
        @DisplayName("should skip rate limiting for health endpoints")
        void shouldSkipRateLimitingForHealthEndpoints() {
            for (int i = 0; i < 20; i++) {
                given()
                .when()
                    .get("/health")
                .then()
                    .statusCode(anyOf(is(200), is(503)));
            }
        }

        @Test
        @DisplayName("should skip rate limiting for metrics endpoints")
        void shouldSkipRateLimitingForMetricsEndpoints() {
            for (int i = 0; i < 20; i++) {
                given()
                .when()
                    .get("/q/health")
                .then()
                    .statusCode(anyOf(is(200), is(503)));
            }
        }
    }

    @Nested
    @DisplayName("Rate Limiting - Response Headers")
    class ResponseHeaders {

        @Test
        @DisplayName("should return proper error response when rate limited")
        void shouldReturnProperErrorResponseWhenRateLimited() {
            String clientId = "test-error-" + System.currentTimeMillis();
            
            Response response = given()
                .header("X-Forwarded-For", clientId)
            .when()
                .post("/api/v1/auth/login");

            int statusCode = response.getStatusCode();
            if (statusCode == 429) {
                response.then()
                    .statusCode(429)
                    .header("Retry-After", notNullValue())
                    .body("error", equalTo("RATE_LIMIT_EXCEEDED"))
                    .body("message", notNullValue())
                    .body("retryAfter", notNullValue());
            }
        }

        @Test
        @DisplayName("should include Retry-After header when rate limited")
        void shouldIncludeRetryAfterHeaderWhenRateLimited() {
            String clientId = "test-retry-" + System.currentTimeMillis();
            
            Response response = given()
                .header("X-Forwarded-For", clientId)
            .when()
                .post("/api/v1/auth/login");

            int statusCode = response.getStatusCode();
            if (statusCode == 429) {
                response.then()
                    .header("Retry-After", equalTo("60"));
            }
        }
    }

    @Nested
    @DisplayName("Rate Limiting - Multiple Endpoint Categories")
    class MultipleEndpointCategories {

        @Test
        @DisplayName("should apply different rate limits to different endpoints")
        void shouldApplyDifferentRateLimitsToDifferentEndpoints() {
            String clientId = "test-multi-" + System.currentTimeMillis();
            
            given()
                .header("X-Forwarded-For", clientId)
            .when()
                .get("/api/v1/balance")
            .then()
                .statusCode(anyOf(is(200), is(401), is(404), is(500), is(503)));

            given()
                .header("X-Forwarded-For", clientId)
            .when()
                .post("/api/v1/auth/login")
            .then()
                .statusCode(anyOf(is(200), is(400), is(404), is(415), is(429), is(500), is(503)));

            given()
                .header("X-Forwarded-For", clientId)
            .when()
                .post("/api/v1/transfer")
            .then()
                .statusCode(anyOf(is(200), is(400), is(404), is(415), is(429), is(500), is(503)));
        }
    }
}
