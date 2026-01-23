package id.payu.portal.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class ApiPortalResourceTest {

    @Test
    void testListServices() {
        given()
            .when().get("/api/v1/portal/services")
            .then()
            .statusCode(200)
            .body("services", not(empty()));
    }

    @Test
    void testGetAggregatedSpecs() {
        given()
            .when().get("/api/v1/portal/openapi")
            .then()
            .statusCode(200)
            .body("version", equalTo("1.0.0"));
    }

    @Test
    void testRefreshSpecs() {
        given()
            .contentType("application/json")
            .when().post("/api/v1/portal/refresh")
            .then()
            .statusCode(200)
            .body("version", equalTo("1.0.0"));
    }

    @Test
    void testHealthEndpoint() {
        given()
            .when().get("/q/health")
            .then()
            .statusCode(200);
    }

    @Test
    void testHealthLiveness() {
        given()
            .when().get("/q/health/live")
            .then()
            .statusCode(200)
            .body("status", equalTo("UP"));
    }

    @Test
    void testHealthReadiness() {
        given()
            .when().get("/q/health/ready")
            .then()
            .statusCode(200)
            .body("status", equalTo("UP"));
    }
}
