package id.payu.gateway.adapter.filter;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestProfile(CorsFilterTestProfile.class)
@DisplayName("CORS Filter Tests")
class CorsFilterTest {

    @Nested
    @DisplayName("Origin Validation - Integration")
    class OriginValidationIntegration {

        @Test
        @DisplayName("should allow requests from allowed origin")
        void shouldAllowRequestsFromAllowedOrigin() {
            given()
                .header("Origin", "https://payu.fajjjar.my.id")
            .when()
                .get("/api/v1/partners")
            .then()
                .header("Access-Control-Allow-Origin", "https://payu.fajjjar.my.id")
                .statusCode(anyOf(is(200), is(404), is(429), is(503)));
        }

        @Test
        @DisplayName("should allow requests from localhost")
        void shouldAllowRequestsFromLocalhost() {
            given()
                .header("Origin", "http://localhost:3000")
            .when()
                .get("/api/v1/partners")
            .then()
                .header("Access-Control-Allow-Origin", "http://localhost:3000")
                .statusCode(anyOf(is(200), is(404), is(429), is(503)));
        }

        @Test
        @DisplayName("should reject requests from disallowed origin")
        void shouldRejectRequestsFromDisallowedOrigin() {
            given()
                .header("Origin", "https://malicious.com")
            .when()
                .get("/api/v1/partners")
            .then()
                .header("Access-Control-Allow-Origin", nullValue())
                .statusCode(anyOf(is(200), is(404), is(429), is(503)));
        }
    }

    @Nested
    @DisplayName("CORS Headers - Integration")
    class CorsHeadersIntegration {

        @Test
        @DisplayName("should include all required CORS headers")
        void shouldIncludeAllRequiredCorsHeaders() {
            given()
                .header("Origin", "https://payu.fajjjar.my.id")
            .when()
                .options("/api/v1/partners")
            .then()
                .header("Access-Control-Allow-Methods", notNullValue())
                .header("Access-Control-Allow-Headers", notNullValue())
                .header("Access-Control-Expose-Headers", notNullValue())
                .header("Access-Control-Allow-Credentials", "true")
                .header("Access-Control-Max-Age", notNullValue());
        }

        @Test
        @DisplayName("should include allow credentials header")
        void shouldIncludeAllowCredentialsHeader() {
            given()
                .header("Origin", "https://payu.fajjjar.my.id")
            .when()
                .get("/api/v1/partners")
            .then()
                .header("Access-Control-Allow-Credentials", "true");
        }

        @Test
        @DisplayName("should handle preflight requests")
        void shouldHandlePreflightRequests() {
            given()
                .header("Origin", "https://payu.fajjjar.my.id")
                .header("Access-Control-Request-Method", "POST")
            .when()
                .options("/api/v1/partners")
            .then()
                .header("Access-Control-Allow-Origin", "https://payu.fajjjar.my.id")
                .header("Access-Control-Allow-Methods", containsString("POST"));
        }
    }
}
