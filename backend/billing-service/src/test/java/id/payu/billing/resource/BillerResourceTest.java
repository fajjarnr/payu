package id.payu.billing.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@DisplayName("Biller Resource Tests")
class BillerResourceTest {

    @Test
    @DisplayName("GET /api/v1/billers - should return all billers")
    void shouldReturnAllBillers() {
        given()
            .when()
            .get("/api/v1/billers")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", hasSize(greaterThan(0)))
            .body("[0].code", notNullValue())
            .body("[0].displayName", notNullValue());
    }

    @Test
    @DisplayName("GET /api/v1/billers?category=mobile - should filter by category")
    void shouldFilterByCategory() {
        given()
            .queryParam("category", "mobile")
            .when()
            .get("/api/v1/billers")
            .then()
            .statusCode(200)
            .body("$", hasSize(greaterThan(0)))
            .body("category", everyItem(equalTo("mobile")));
    }

    @Test
    @DisplayName("GET /api/v1/billers/{code} - should return specific biller")
    void shouldReturnSpecificBiller() {
        given()
            .when()
            .get("/api/v1/billers/PLN")
            .then()
            .statusCode(200)
            .body("code", equalTo("PLN"))
            .body("displayName", containsString("PLN"))
            .body("category", equalTo("electricity"));
    }

    @Test
    @DisplayName("GET /api/v1/billers/{code} - should return 404 for unknown biller")
    void shouldReturn404ForUnknownBiller() {
        given()
            .when()
            .get("/api/v1/billers/UNKNOWN")
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("GET /api/v1/billers/categories - should return all categories")
    void shouldReturnAllCategories() {
        given()
            .when()
            .get("/api/v1/billers/categories")
            .then()
            .statusCode(200)
            .body("$", hasItems("electricity", "water", "mobile"));
    }
}
