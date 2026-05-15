package id.payu.portal.adapter.web;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestSecurity(authorizationEnabled = false)
@DisplayName("API Portal Resource Tests")
class ApiPortalResourceTest {

    @Test
    @DisplayName("should list all registered services")
    void testListServices() {
        given()
            .when().get("/api/v1/portal/services")
            .then()
            .statusCode(200)
            .body("services", not(empty()));
    }

    @Test
    @DisplayName("should return aggregated OpenAPI specs")
    void testGetAggregatedSpecs() {
        // Use refresh=true for test-order independence (avoids stale cache TTL parse)
        given()
            .queryParam("refresh", "true")
            .when().get("/api/v1/portal/openapi")
            .then()
            .statusCode(200)
            .body("version", equalTo("1.0.0"));
    }

    @Test
    @DisplayName("should refresh specs via POST endpoint")
    void testRefreshSpecs() {
        given()
            .contentType("application/json")
            .when().post("/api/v1/portal/refresh")
            .then()
            .statusCode(200)
            .body("version", equalTo("1.0.0"));
    }

    @Test
    @DisplayName("should return health status UP")
    void testHealthEndpoint() {
        given()
            .when().get("/q/health")
            .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("should return liveness status UP")
    void testHealthLiveness() {
        given()
            .when().get("/q/health/live")
            .then()
            .statusCode(200)
            .body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("should return readiness status UP")
    void testHealthReadiness() {
        given()
            .when().get("/q/health/ready")
            .then()
            .statusCode(200)
            .body("status", equalTo("UP"));
    }

    // ────────── NEW TESTS ──────────

    @Test
    @DisplayName("should return OpenAPI spec for known service (or 404 if unreachable)")
    void testGetServiceSpec_ForExistingService() {
        // account-service is in the config but unreachable at localhost:8080
        // When unreachable: spec is null -> resource returns 404
        given()
            .when().get("/api/v1/portal/services/account-service/openapi")
            .then()
            .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    @Test
    @DisplayName("should return 500 when service config is not found")
    void testGetServiceSpec_NonExistentService() {
        given()
            .when().get("/api/v1/portal/services/non-existent-service/openapi")
            .then()
            .statusCode(500);
    }

    @Test
    @DisplayName("should refresh aggregated specs with refresh=true query param")
    void testGetAggregatedSpecs_ForceRefresh() {
        // Use refresh=true to skip the cache entirely (calls refreshCache directly)
        given()
            .queryParam("refresh", "true")
            .when().get("/api/v1/portal/openapi")
            .then()
            .statusCode(200)
            .body("version", equalTo("1.0.0"))
            .body("services", notNullValue());
    }

    @Test
    @DisplayName("should return HTML index page from Swagger UI")
    @Tag("ui")
    void testSwaggerUi_IndexPage() {
        given()
            .when().get("/")
            .then()
            .statusCode(200)
            .contentType(containsString("text/html"))
            .body(containsString("PayU API Portal"))
            .body(containsString("Available Services"))
            .body(containsString("api/v1/portal/openapi"));
    }

    @Test
    @DisplayName("should return HTML service page with Swagger UI")
    @Tag("ui")
    void testSwaggerUi_ServicePage() {
        given()
            .when().get("/service/account-service")
            .then()
            .statusCode(200)
            .contentType(containsString("text/html"))
            .body(containsString("account-service"))
            .body(containsString("swagger-ui"));
    }

    @Test
    @DisplayName("should return HTML for non-existent service page")
    @Tag("ui")
    void testSwaggerUi_UnknownServicePage() {
        given()
            .when().get("/service/unknown-service")
            .then()
            .statusCode(200)
            .contentType(containsString("text/html"))
            .body(containsString("Back to Portal"));
    }

    @Test
    @DisplayName("should expose built-in OpenAPI endpoint in JSON format")
    @Tag("contract")
    void testBuiltInOpenApiEndpoint() {
        // /q/openapi returns YAML by default; request JSON explicitly
        given()
            .accept(ContentType.JSON)
            .when().get("/q/openapi")
            .then()
            .statusCode(200)
            .body("openapi", notNullValue())
            .body("info.title", equalTo("PayU API Portal"))
            .body("paths", notNullValue());
    }

    @Test
    @DisplayName("should list services with all required fields populated")
    void testListServices_HasRequiredFields() {
        given()
            .when().get("/api/v1/portal/services")
            .then()
            .statusCode(200)
            .body("services[0].id", notNullValue())
            .body("services[0].name", notNullValue())
            .body("services[0].url", notNullValue())
            .body("services[0].openapiPath", notNullValue())
            .body("services[0].status", anyOf(equalTo("UP"), equalTo("DOWN"), equalTo("UNKNOWN")));
    }

    @Test
    @DisplayName("should return valid JSON from aggregated specs via direct refresh")
    void testAggregatedSpecs_ValidJsonViaRefresh() {
        // POST to /api/v1/portal/refresh is safe to call repeatedly
        given()
            .contentType("application/json")
            .when().post("/api/v1/portal/refresh")
            .then()
            .statusCode(200)
            .body("version", equalTo("1.0.0"))
            .body("services", notNullValue())
            .body("lastUpdated", notNullValue());
    }
}
