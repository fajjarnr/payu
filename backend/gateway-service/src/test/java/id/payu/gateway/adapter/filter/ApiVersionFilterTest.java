package id.payu.gateway.adapter.filter;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestProfile(ApiVersionFilterTestProfile.class)
@DisplayName("API Versioning Filter Tests")
public class ApiVersionFilterTest {

    @Test
    @DisplayName("Should accept valid API version in path")
    public void testValidVersionInPath() {
        given()
            .when()
            .get("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(200), is(404), is(401), is(429), is(503)));
    }

    @Test
    @DisplayName("Should accept valid API version in header")
    public void testValidVersionInHeader() {
        given()
            .header("X-API-Version", "v1")
            .when()
            .get("/api/v1/accounts")
            .then()
            .statusCode(anyOf(is(200), is(404), is(401), is(429), is(503)));
    }

    @Test
    @DisplayName("Should reject invalid API version")
    public void testInvalidVersion() {
        // Use a path that does NOT contain an embedded version (e.g., /gateway/...)
        // so ApiVersionFilter falls back to the X-API-Version header.
        // Paths like /api/v1/accounts have "v1" extracted from the path first,
        // making the header irrelevant.
        given()
            .header("X-API-Version", "v99")
            .when()
            .get("/gateway/analytics/health")
            .then()
            .statusCode(400)
            .body("error", equalTo("INVALID_API_VERSION"));
    }

    @Test
    @DisplayName("Should use default version when no version specified")
    public void testDefaultVersion() {
        given()
            .when()
            .get("/q/health")
            .then()
            .statusCode(anyOf(is(200), is(503)));
    }

    @Test
    @DisplayName("Should skip version validation for SNAP-BI v1.0 taxonomy (SNAP-PATH-001)")
    public void testSnapBiV10TaxonomyIsNotTreatedAsApiVersion() {
        // /v1.0 is the BI-mandated SNAP-BI taxonomy prefix, not a PayU API version.
        // It must route to partner-service, not be rejected as INVALID_API_VERSION.
        given()
            .when()
            .get("/v1.0/access-token/b2b")
            .then()
            .statusCode(not(is(400)))
            .body(not(containsString("INVALID_API_VERSION")));
    }

    @Test
    @DisplayName("Should skip version validation for legacy SNAP-BI /v1/partner contract")
    public void testLegacySnapBiContractIsNotTreatedAsApiVersion() {
        given()
            .when()
            .get("/v1/partner/auth/token")
            .then()
            .statusCode(not(is(400)))
            .body(not(containsString("INVALID_API_VERSION")));
    }
}
