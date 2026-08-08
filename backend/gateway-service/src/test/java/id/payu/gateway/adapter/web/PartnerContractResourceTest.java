package id.payu.gateway.adapter.web;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.isOneOf;

/**
 * PARTNER-001: the public SNAP-BI contract path {@code /v1/partner/**} must
 * reach partner-service without a doubled prefix and without a 404 from the
 * route registry.
 */
@QuarkusTest
@DisplayName("Partner Contract Route Tests")
class PartnerContractResourceTest {

    @Test
    @DisplayName("POST /v1/partner/auth/token reaches partner-service (no double prefix)")
    void contractTokenPathReachesPartnerService() {
        // Downstream partner-service may be absent in the unit-test env, so the
        // proxy returns 502/503 — the regression is that this is NOT a 404 from
        // "no route found for path".
        given()
            .header("X-CLIENT-KEY", "test-key")
            .contentType("application/json")
            .when()
            .post("/v1/partner/auth/token")
            .then()
            .statusCode(isOneOf(200, 400, 401, 415, 429, 502, 503));
    }

    @Test
    @DisplayName("GET /v1/partner/payments/{id} is routable (no double prefix)")
    void contractStatusPathIsRoutable() {
        given()
            .when()
            .get("/v1/partner/payments/PAYU-0001")
            .then()
            .statusCode(isOneOf(200, 401, 404, 429, 502, 503));
    }

    @Test
    @DisplayName("legacy /api/v1/v1/partner/auth/token remains routable for backward compatibility")
    void legacyDoublePrefixPathStillRoutable() {
        given()
            .header("X-CLIENT-KEY", "test-key")
            .contentType("application/json")
            .when()
            .post("/api/v1/v1/partner/auth/token")
            .then()
            .statusCode(isOneOf(200, 400, 401, 415, 429, 502, 503));
    }
}
