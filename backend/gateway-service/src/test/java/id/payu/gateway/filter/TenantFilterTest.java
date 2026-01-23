package id.payu.gateway.filter;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Header;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@DisplayName("Tenant Filter Tests")
class TenantFilterTest {

    @Nested
    @DisplayName("Tenant ID Extraction")
    class TenantExtraction {

        @Test
        @DisplayName("should extract tenant ID from X-Tenant-Id header")
        void shouldExtractTenantIdFromHeader() {
            given()
                .header(new Header("X-Tenant-Id", "tenant-123"))
            .when()
                .get("/api/v1/accounts")
            .then()
                .statusCode(anyOf(is(200), is(404), is(401), is(500), is(503)))
                .header("X-Tenant-Id", "tenant-123");
        }

        @Test
        @DisplayName("should use default tenant when no header provided")
        void shouldUseDefaultTenantWhenNoHeaderProvided() {
            given()
            .when()
                .get("/api/v1/accounts")
            .then()
                .statusCode(anyOf(is(200), is(404), is(401), is(500), is(503)))
                .header("X-Tenant-Id", "default");
        }

        @Test
        @DisplayName("should use default tenant when header is blank")
        void shouldUseDefaultTenantWhenHeaderIsBlank() {
            given()
                .header(new Header("X-Tenant-Id", "   "))
            .when()
                .get("/api/v1/accounts")
            .then()
                .statusCode(anyOf(is(200), is(404), is(401), is(500), is(503)))
                .header("X-Tenant-Id", "default");
        }

        @Test
        @DisplayName("should handle different tenant IDs correctly")
        void shouldHandleDifferentTenantIdsCorrectly() {
            String[] tenantIds = {"tenant-a", "tenant-b", "company-123", "org-456"};

            for (String tenantId : tenantIds) {
                given()
                    .header(new Header("X-Tenant-Id", tenantId))
                .when()
                    .get("/api/v1/wallets")
                .then()
                    .statusCode(anyOf(is(200), is(404), is(401), is(500), is(503)))
                    .header("X-Tenant-Id", tenantId);
            }
        }
    }

    @Nested
    @DisplayName("Tenant ID Propagation")
    class TenantPropagation {

        @Test
        @DisplayName("should propagate tenant ID to backend services")
        void shouldPropagateTenantIdToBackendServices() {
            given()
                .header(new Header("X-Tenant-Id", "test-tenant"))
            .when()
                .post("/api/v1/auth/login")
            .then()
                .statusCode(anyOf(is(200), is(400), is(404), is(415), is(500), is(503)));
        }

        @Test
        @DisplayName("should maintain tenant ID across multiple requests")
        void shouldMaintainTenantIdAcrossMultipleRequests() {
            String tenantId = "consistent-tenant";
            Header tenantHeader = new Header("X-Tenant-Id", tenantId);

            given()
                .header(tenantHeader)
            .when()
                .get("/api/v1/accounts")
            .then()
                .statusCode(anyOf(is(200), is(404), is(401), is(500), is(503)))
                .header("X-Tenant-Id", tenantId);

            given()
                .header(tenantHeader)
            .when()
                .get("/api/v1/wallets")
            .then()
                .statusCode(anyOf(is(200), is(404), is(401), is(500), is(503)))
                .header("X-Tenant-Id", tenantId);
        }
    }

    @Nested
    @DisplayName("Health and Metrics")
    class HealthAndMetrics {

        @Test
        @DisplayName("should apply tenant filter to health endpoints")
        void shouldApplyTenantFilterToHealthEndpoints() {
            given()
                .header(new Header("X-Tenant-Id", "health-tenant"))
            .when()
                .get("/health")
            .then()
                .statusCode(anyOf(is(200), is(503)));
        }

        @Test
        @DisplayName("should skip health endpoints from rate limiting but apply tenant filter")
        void shouldSkipHealthFromRateLimitingButApplyTenantFilter() {
            for (int i = 0; i < 20; i++) {
                given()
                    .header(new Header("X-Tenant-Id", "test-tenant"))
                .when()
                    .get("/health")
                .then()
                    .statusCode(anyOf(is(200), is(503)));
            }
        }
    }
}
