package id.payu.simulator.biller;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

/**
 * Deterministic bill payment simulation tests (seeded accounts from DataInitializer).
 */
@QuarkusTest
class BillerSimulatorTest {

    @Test
    void inquiryReturnsOutstanding() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("billerCode", "PLN", "customerNumber", "PLN-001234567890"))
            .when()
            .post("/api/v1/biller/inquiry")
            .then()
            .statusCode(200)
            .body("billerCode", equalTo("PLN"))
            .body("customerName", equalTo("JOHN DOE"))
            .body("outstandingAmount", equalTo(350000.00f));
    }

    @Test
    void inquiryNotFound() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("billerCode", "PLN", "customerNumber", "PLN-000000000000"))
            .when()
            .post("/api/v1/biller/inquiry")
            .then()
            .statusCode(400)
            .body("responseCode", equalTo("14"));
    }

    @Test
    void paySettlesOutstanding() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "billerCode", "PDAM",
                "customerNumber", "PDAM-001234567890",
                "amount", 125000,
                "referenceNumber", "REF-001"))
            .when()
            .post("/api/v1/biller/pay")
            .then()
            .statusCode(200)
            .body("responseCode", equalTo("00"))
            .body("status", equalTo("COMPLETED"));
    }

    @Test
    void healthIsUp() {
        given()
            .when()
            .get("/api/v1/biller/health")
            .then()
            .statusCode(200)
            .body("status", equalTo("UP"));
    }
}
