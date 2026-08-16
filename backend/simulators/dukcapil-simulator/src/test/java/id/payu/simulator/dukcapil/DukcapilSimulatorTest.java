package id.payu.simulator.dukcapil;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

/**
 * Deterministic eKYC simulation tests (seeded citizens from DataInitializer).
 */
@QuarkusTest
class DukcapilSimulatorTest {

    @Test
    void verifyValidNikMatchesName() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "nik", "3201234567890001",
                "fullName", "JOHN DOE",
                "birthPlace", "JAKARTA",
                "birthDate", "1990-01-15"))
            .when()
            .post("/api/v1/verify")
            .then()
            .statusCode(200)
            .body("responseCode", equalTo("00"))
            .body("verified", equalTo(true));
    }

    @Test
    void verifyNikNotFound() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("nik", "9999999999999999", "fullName", "NONEXISTENT"))
            .when()
            .post("/api/v1/verify")
            .then()
            .statusCode(404)
            .body("responseCode", equalTo("14"));
    }

    @Test
    void verifyBlockedNik() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("nik", "3201234567890003", "fullName", "BLOCKED USER"))
            .when()
            .post("/api/v1/verify")
            .then()
            .statusCode(403)
            .body("responseCode", equalTo("62"));
    }

    @Test
    void verifyInvalidFormat() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("nik", "123", "fullName", "JOHN DOE"))
            .when()
            .post("/api/v1/verify")
            .then()
            .statusCode(400)
            .body(containsString("16"));
    }

    @Test
    void getCitizenDataByNik() {
        given()
            .when()
            .get("/api/v1/nik/3201234567890001")
            .then()
            .statusCode(200)
            .body("nik", equalTo("3201234567890001"))
            .body("fullName", equalTo("JOHN DOE"));
    }
}
