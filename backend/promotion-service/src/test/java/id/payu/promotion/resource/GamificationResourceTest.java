package id.payu.promotion.resource;

import id.payu.promotion.dto.*;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestHTTPEndpoint(GamificationResource.class)
class GamificationResourceTest {

    private static final String TEST_ACCOUNT_ID = "acc-integration-123";

    @BeforeEach
    void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void testPerformDailyCheckin_Success() {
        given()
            .queryParam("accountId", TEST_ACCOUNT_ID)
            .when()
            .post("/checkin")
            .then()
            .statusCode(201)
            .body("accountId", equalTo(TEST_ACCOUNT_ID))
            .body("checkinDate", equalTo(LocalDate.now().toString()))
            .body("streakCount", equalTo(1))
            .body("pointsEarned", greaterThan(0));
    }

    @Test
    void testPerformDailyCheckin_AlreadyCheckedIn_Returns400() {
        given()
            .queryParam("accountId", TEST_ACCOUNT_ID)
            .post("/checkin");

        given()
            .queryParam("accountId", TEST_ACCOUNT_ID)
            .when()
            .post("/checkin")
            .then()
            .statusCode(400)
            .body("message", containsString("Already checked in"));
    }

    @Test
    void testGetTodayCheckin_CheckedIn() {
        given()
            .queryParam("accountId", TEST_ACCOUNT_ID)
            .post("/checkin");

        given()
            .queryParam("accountId", TEST_ACCOUNT_ID)
            .when()
            .get("/checkin/today")
            .then()
            .statusCode(200)
            .body("accountId", equalTo(TEST_ACCOUNT_ID))
            .body("checkinDate", equalTo(LocalDate.now().toString()));
    }

    @Test
    void testGetTodayCheckin_NotCheckedIn_ReturnsNull() {
        given()
            .queryParam("accountId", TEST_ACCOUNT_ID)
            .when()
            .get("/checkin/today")
            .then()
            .statusCode(200)
            .body(nullValue());
    }

    @Test
    void testGetCurrentStreak_NoCheckins() {
        given()
            .queryParam("accountId", TEST_ACCOUNT_ID)
            .when()
            .get("/checkin/streak")
            .then()
            .statusCode(200)
            .body(equalTo("0"));
    }

    @Test
    void testGetCurrentStreak_WithCheckin() {
        given()
            .queryParam("accountId", TEST_ACCOUNT_ID)
            .post("/checkin");

        given()
            .queryParam("accountId", TEST_ACCOUNT_ID)
            .when()
            .get("/checkin/streak")
            .then()
            .statusCode(200)
            .body(equalTo("1"));
    }

    @Test
    void testProcessTransaction_Success() {
        var request = new ProcessTransactionRequest(
            TEST_ACCOUNT_ID,
            "txn-integration-001",
            BigDecimal.valueOf(50000),
            "MERCHANT1",
            "FOOD"
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/transaction")
            .then()
            .statusCode(200)
            .body("xpEarned", greaterThan(0));
    }

    @Test
    void testProcessTransaction_Duplicate_Succeeds() {
        var request = new ProcessTransactionRequest(
            TEST_ACCOUNT_ID,
            "txn-integration-001",
            BigDecimal.valueOf(50000),
            "MERCHANT1",
            "FOOD"
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .post("/transaction");

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/transaction")
            .then()
            .statusCode(200)
            .body("xpEarned", equalTo(0));
    }

    @Test
    void testGetUserLevel_NotExists_Returns404() {
        given()
            .queryParam("accountId", "non-existent-account")
            .when()
            .get("/level")
            .then()
            .statusCode(404)
            .body("message", equalTo("User level not found"));
    }

    @Test
    void testGetUserLevel_Exists() {
        var request = new ProcessTransactionRequest(
            TEST_ACCOUNT_ID,
            "txn-integration-001",
            BigDecimal.valueOf(50000),
            "MERCHANT1",
            "FOOD"
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .post("/transaction");

        given()
            .queryParam("accountId", TEST_ACCOUNT_ID)
            .when()
            .get("/level")
            .then()
            .statusCode(200)
            .body("accountId", equalTo(TEST_ACCOUNT_ID))
            .body("level", greaterThan(1))
            .body("levelName", notNullValue())
            .body("xp", greaterThan(0));
    }

    @Test
    void testGetUserBadges_NoneEarned() {
        given()
            .queryParam("accountId", TEST_ACCOUNT_ID)
            .when()
            .get("/badges")
            .then()
            .statusCode(200)
            .body("", hasSize(0));
    }

    @Test
    void testGetUserBadges_WithBadges() {
        var request = new ProcessTransactionRequest(
            TEST_ACCOUNT_ID,
            "txn-integration-001",
            BigDecimal.valueOf(50000),
            "MERCHANT1",
            "FOOD"
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .post("/transaction");

        given()
            .queryParam("accountId", TEST_ACCOUNT_ID)
            .when()
            .get("/badges")
            .then()
            .statusCode(200)
            .body("", hasSize(greaterThan(0)));
    }

    @Test
    void testGetBadgeProgress() {
        given()
            .queryParam("accountId", TEST_ACCOUNT_ID)
            .when()
            .get("/badges/progress")
            .then()
            .statusCode(200)
            .body("", is(notNullValue()));
    }

    @Test
    void testGetSummary() {
        given()
            .queryParam("accountId", TEST_ACCOUNT_ID)
            .post("/checkin");

        var request = new ProcessTransactionRequest(
            TEST_ACCOUNT_ID,
            "txn-integration-001",
            BigDecimal.valueOf(50000),
            "MERCHANT1",
            "FOOD"
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .post("/transaction");

        given()
            .queryParam("accountId", TEST_ACCOUNT_ID)
            .when()
            .get("/summary")
            .then()
            .statusCode(200)
            .body("level", notNullValue())
            .body("badges", notNullValue())
            .body("lastCheckin", notNullValue())
            .body("currentStreak", equalTo(1))
            .body("totalCheckins", equalTo(1));
    }
}
