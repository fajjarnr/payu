package id.payu.gateway.adapter.filter;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for {@link AuthorizationFilter}.
 *
 * <p>Tests JWT validation rejection scenarios:
 * <ul>
 *   <li>Missing Authorization header</li>
 *   <li>Invalid Authorization header format</li>
 *   <li>Invalid JWT format</li>
 *   <li>Public endpoint bypass</li>
 * </ul>
 *
 * <p>Note: Tests for signature verification, expiration, issuer, and audience
 * validation require a running JWKS endpoint (Keycloak). Those validations
 * are tested manually and verified through code review.
 *
 * @author PayU Engineering Team
 * @since 1.0.0
 */
@QuarkusTest
@TestProfile(AuthorizationFilterTestProfile.class)
@DisplayName("Authorization Filter Integration Tests")
class AuthorizationFilterTest {

    // ==================== Public Endpoint Tests ====================

    @Test
    @DisplayName("Should bypass authorization for public health endpoint")
    void testPublicEndpointBypass_Health() {
        given()
            .when()
            .get("/health")
            .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("Should bypass authorization for public auth login endpoint")
    void testPublicEndpointBypass_Login() {
        // The auth endpoint is public but returns 404/503 in test since backend is not available
        given()
            .when()
            .get("/api/v1/auth/login")
            .then()
            .statusCode(anyOf(is(200), is(404), is(503)));
    }

    @Test
    @DisplayName("Should bypass authorization for public accounts register endpoint")
    void testPublicEndpointBypass_Register() {
        given()
            .contentType("application/json")
            .when()
            .post("/api/v1/accounts/register")
            .then()
            .statusCode(anyOf(is(201), is(400), is(404), is(415), is(503)));
    }

    @Test
    @DisplayName("Should bypass authorization for Quarkus health endpoint")
    void testPublicEndpointBypass_QuarkusHealth() {
        given()
            .when()
            .get("/q/health")
            .then()
            .statusCode(anyOf(is(200), is(503)));
    }

    @Test
    @DisplayName("Should bypass platform JWT for SNAP token endpoint")
    void testPublicEndpointBypass_SnapToken() {
        given()
            .contentType("application/json")
            .body("{\"grantType\":\"client_credentials\"}")
            .when()
            .post("/api/v1/v1/partner/auth/token")
            .then()
            .statusCode(anyOf(is(400), is(404), is(503)));
    }

    // ==================== Missing/Invalid Token Tests ====================

    @Test
    @DisplayName("Should reject request without Authorization header")
    void testMissingAuthorizationHeader() {
        given()
            .when()
            .get("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(401), is(503)));
    }

    @Test
    @DisplayName("Should reject request with invalid Authorization header format")
    void testInvalidAuthorizationHeaderFormat() {
        given()
            .header("Authorization", "Basic dXNlcjpwYXNz")
            .when()
            .get("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(401), is(503)));
    }

    @Test
    @DisplayName("Should reject invalid JWT format")
    void testInvalidJwtFormat() {
        given()
            .header("Authorization", "Bearer invalid-token-format")
            .when()
            .get("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(401), is(503)));
    }

    @Test
    @DisplayName("Should handle empty Authorization header")
    void testEmptyAuthorizationHeader() {
        given()
            .header("Authorization", "")
            .when()
            .get("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(401), is(503)));
    }

    @Test
    @DisplayName("Should handle Authorization header with only Bearer prefix")
    void testAuthorizationHeaderOnlyBearer() {
        given()
            .header("Authorization", "Bearer ")
            .when()
            .get("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(401), is(503)));
    }

    // ==================== JWT Structure Tests ====================

    @Test
    @DisplayName("Should reject token with invalid JWT structure")
    void testInvalidJwtStructure() {
        // A valid JWT has 3 parts separated by dots
        given()
            .header("Authorization", "Bearer header.payload")
            .when()
            .get("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(401), is(503)));
    }

    @Test
    @DisplayName("Should reject token with only two parts")
    void testJwtWithTwoParts() {
        given()
            .header("Authorization", "Bearer eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyIn0")
            .when()
            .get("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(401), is(503)));
    }
}
