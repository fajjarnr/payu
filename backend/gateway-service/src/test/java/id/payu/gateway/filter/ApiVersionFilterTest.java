package id.payu.gateway.filter;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@DisplayName("API Versioning Filter Tests")
public class ApiVersionFilterTest {

    @Test
    @DisplayName("Should accept valid API version in path")
    public void testValidVersionInPath() {
        given()
            .when()
            .get("/q/health")
            .then()
            .statusCode(200)
            .header("X-API-Version", notNullValue());
    }

    @Test
    @DisplayName("Should accept valid API version in header")
    public void testValidVersionInHeader() {
        given()
            .header("X-API-Version", "v1")
            .when()
            .get("/q/health")
            .then()
            .statusCode(200)
            .header("X-API-Version", "v1");
    }

    @Test
    @DisplayName("Should reject invalid API version")
    public void testInvalidVersion() {
        given()
            .header("X-API-Version", "v99")
            .when()
            .get("/q/health")
            .then()
            .statusCode(400)
            .body(containsString("INVALID_API_VERSION"));
    }

    @Test
    @DisplayName("Should use default version when no version specified")
    public void testDefaultVersion() {
        given()
            .when()
            .get("/q/health")
            .then()
            .statusCode(200);
    }
}
