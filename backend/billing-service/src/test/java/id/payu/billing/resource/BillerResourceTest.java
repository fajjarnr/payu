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

    @Test
    @DisplayName("GET /api/v1/billers?category=tv_cable - should return TV Cable billers")
    void shouldReturnTVCableBillers() {
        given()
            .queryParam("category", "tv_cable")
            .when()
            .get("/api/v1/billers")
            .then()
            .statusCode(200)
            .body("$", hasSize(greaterThan(0)))
            .body("category", everyItem(equalTo("tv_cable")))
            .body("code", hasItems("INDOVISION", "TRANSTV", "KVISION", "MNC_VISION"));
    }

    @Test
    @DisplayName("GET /api/v1/billers?category=multifinance - should return Multifinance billers")
    void shouldReturnMultifinanceBillers() {
        given()
            .queryParam("category", "multifinance")
            .when()
            .get("/api/v1/billers")
            .then()
            .statusCode(200)
            .body("$", hasSize(greaterThan(0)))
            .body("category", everyItem(equalTo("multifinance")))
            .body("code", hasItems("FIFASTRA", "BFI", "ADIRA", "WOM", "MEGA_FINANCE"));
    }

    @Test
    @DisplayName("GET /api/v1/billers/{code} - should return TV Cable biller with correct admin fee")
    void shouldReturnTVCableBillerWithCorrectAdminFee() {
        given()
            .when()
            .get("/api/v1/billers/INDOVISION")
            .then()
            .statusCode(200)
            .body("code", equalTo("INDOVISION"))
            .body("displayName", containsString("Indovision"))
            .body("category", equalTo("tv_cable"))
            .body("adminFee", equalTo(2500));
    }

    @Test
    @DisplayName("GET /api/v1/billers/{code} - should return Multifinance biller with correct admin fee")
    void shouldReturnMultifinanceBillerWithCorrectAdminFee() {
        given()
            .when()
            .get("/api/v1/billers/FIFASTRA")
            .then()
            .statusCode(200)
            .body("code", equalTo("FIFASTRA"))
            .body("displayName", containsString("FIFASTRA"))
            .body("category", equalTo("multifinance"))
            .body("adminFee", equalTo(5000));
    }

    @Test
    @DisplayName("GET /api/v1/billers/categories - should include tv_cable and multifinance categories")
    void shouldIncludeNewCategories() {
        given()
            .when()
            .get("/api/v1/billers/categories")
            .then()
            .statusCode(200)
            .body("$", hasItems("electricity", "water", "mobile", "tv_cable", "multifinance"));
    }
}
