package id.payu.billing.resource;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Biller Resource Tests")
class BillerResourceTest {

    @LocalServerPort
    int port;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        Jwt mockJwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("test-user")
                .claim("scope", "openid")
                .build();
        org.mockito.Mockito.when(jwtDecoder.decode("test-token")).thenReturn(mockJwt);
    }

    private RequestSpecification givenAuth() {
        return given().header("Authorization", "Bearer test-token");
    }

    @Test
    @DisplayName("GET /api/v1/billers - should return all billers")
    void shouldReturnAllBillers() {
        givenAuth()
            .when()
            .get("/api/v1/billers")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", hasSize(greaterThan(0)))
            .body("data[0].code", notNullValue())
            .body("data[0].displayName", notNullValue());
    }

    @Test
    @DisplayName("GET /api/v1/billers?category=mobile - should filter by category")
    void shouldFilterByCategory() {
        givenAuth()
            .queryParam("category", "mobile")
            .when()
            .get("/api/v1/billers")
            .then()
            .statusCode(200)
            .body("data", hasSize(greaterThan(0)))
            .body("data.category", everyItem(equalTo("mobile")));
    }

    @Test
    @DisplayName("GET /api/v1/billers/{code} - should return specific biller")
    void shouldReturnSpecificBiller() {
        givenAuth()
            .when()
            .get("/api/v1/billers/PLN")
            .then()
            .statusCode(200)
            .body("data.code", equalTo("PLN"))
            .body("data.displayName", containsString("PLN"))
            .body("data.category", equalTo("electricity"));
    }

    @Test
    @DisplayName("GET /api/v1/billers/{code} - should return 404 for unknown biller")
    void shouldReturn404ForUnknownBiller() {
        givenAuth()
            .when()
            .get("/api/v1/billers/UNKNOWN")
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("GET /api/v1/billers/categories - should return all categories")
    void shouldReturnAllCategories() {
        givenAuth()
            .when()
            .get("/api/v1/billers/categories")
            .then()
            .statusCode(200)
            .body("data", hasItems("electricity", "water", "mobile"));
    }

    @Test
    @DisplayName("GET /api/v1/billers?category=tv_cable - should return TV Cable billers")
    void shouldReturnTVCableBillers() {
        givenAuth()
            .queryParam("category", "tv_cable")
            .when()
            .get("/api/v1/billers")
            .then()
            .statusCode(200)
            .body("data", hasSize(greaterThan(0)))
            .body("data.category", everyItem(equalTo("tv_cable")))
            .body("data.code", hasItems("INDOVISION", "TRANSTV", "KVISION", "MNC_VISION"));
    }

    @Test
    @DisplayName("GET /api/v1/billers?category=multifinance - should return Multifinance billers")
    void shouldReturnMultifinanceBillers() {
        givenAuth()
            .queryParam("category", "multifinance")
            .when()
            .get("/api/v1/billers")
            .then()
            .statusCode(200)
            .body("data", hasSize(greaterThan(0)))
            .body("data.category", everyItem(equalTo("multifinance")))
            .body("data.code", hasItems("FIFASTRA", "BFI", "ADIRA", "WOM", "MEGA_FINANCE"));
    }

    @Test
    @DisplayName("GET /api/v1/billers/{code} - should return TV Cable biller with correct admin fee")
    void shouldReturnTVCableBillerWithCorrectAdminFee() {
        givenAuth()
            .when()
            .get("/api/v1/billers/INDOVISION")
            .then()
            .statusCode(200)
            .body("data.code", equalTo("INDOVISION"))
            .body("data.displayName", containsString("Indovision"))
            .body("data.category", equalTo("tv_cable"))
            .body("data.adminFee", equalTo(2500));
    }

    @Test
    @DisplayName("GET /api/v1/billers/{code} - should return Multifinance biller with correct admin fee")
    void shouldReturnMultifinanceBillerWithCorrectAdminFee() {
        givenAuth()
            .when()
            .get("/api/v1/billers/FIFASTRA")
            .then()
            .statusCode(200)
            .body("data.code", equalTo("FIFASTRA"))
            .body("data.displayName", containsString("FIFASTRA"))
            .body("data.category", equalTo("multifinance"))
            .body("data.adminFee", equalTo(5000));
    }

    @Test
    @DisplayName("GET /api/v1/billers/categories - should include tv_cable and multifinance categories")
    void shouldIncludeNewCategories() {
        givenAuth()
            .when()
            .get("/api/v1/billers/categories")
            .then()
            .statusCode(200)
            .body("data", hasItems("electricity", "water", "mobile", "tv_cable", "multifinance"));
    }
}
