package id.payu.promotion.resource;

import id.payu.promotion.dto.CompleteReferralRequest;
import id.payu.promotion.dto.CreateReferralRequest;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@Disabled("Resource tests require Docker/Testcontainers - disabled when Docker not available")
@TestHTTPEndpoint(ReferralResource.class)
class ReferralResourceTest {

    private static final String REFERRER_ACCOUNT_ID = "acc-referrer";
    private static final String REFEREE_ACCOUNT_ID = "acc-referee";

    @BeforeEach
    void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void testCreateReferral_Success() {
        var request = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            id.payu.promotion.domain.Referral.RewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("referrerAccountId", equalTo(REFERRER_ACCOUNT_ID))
            .body("referrerReward", equalTo(50.00f))
            .body("refereeReward", equalTo(25.00f))
            .body("rewardType", equalTo("CASHBACK"))
            .body("status", equalTo("PENDING"))
            .body("referralCode", notNullValue())
            .body("referralCode", hasLength(8));
    }

    @Test
    void testCreateReferral_WithPointsRewardType() {
        var request = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("1000.00"),
            new BigDecimal("500.00"),
            id.payu.promotion.domain.Referral.RewardType.POINTS,
            LocalDateTime.now().plusMonths(3)
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("rewardType", equalTo("POINTS"))
            .body("status", equalTo("PENDING"));
    }

    @Test
    void testCreateReferral_InvalidRequest_Returns400() {
        var request = new CreateReferralRequest(
            null,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            id.payu.promotion.domain.Referral.RewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    void testCompleteReferral_Success() {
        var createRequest = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            id.payu.promotion.domain.Referral.RewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        var referralCode = given()
            .contentType(ContentType.JSON)
            .body(createRequest)
            .post()
            .jsonPath()
            .getString("referralCode");

        var completeRequest = new CompleteReferralRequest(
            referralCode,
            REFEREE_ACCOUNT_ID
        );

        given()
            .contentType(ContentType.JSON)
            .body(completeRequest)
            .when()
            .post("/complete")
            .then()
            .statusCode(200)
            .body("refereeAccountId", equalTo(REFEREE_ACCOUNT_ID))
            .body("status", equalTo("COMPLETED"))
            .body("completedAt", notNullValue());
    }

    @Test
    void testCompleteReferral_InvalidCode_Returns400() {
        var request = new CompleteReferralRequest(
            "INVALID_CODE",
            REFEREE_ACCOUNT_ID
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/complete")
            .then()
            .statusCode(400)
            .body("message", equalTo("Invalid referral code"));
    }

    @Test
    void testCompleteReferral_AlreadyCompleted_Returns400() {
        var createRequest = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            id.payu.promotion.domain.Referral.RewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        var referralCode = given()
            .contentType(ContentType.JSON)
            .body(createRequest)
            .post()
            .jsonPath()
            .getString("referralCode");

        var completeRequest = new CompleteReferralRequest(
            referralCode,
            REFEREE_ACCOUNT_ID
        );

        given()
            .contentType(ContentType.JSON)
            .body(completeRequest)
            .post("/complete");

        given()
            .contentType(ContentType.JSON)
            .body(completeRequest)
            .when()
            .post("/complete")
            .then()
            .statusCode(400)
            .body("message", equalTo("Referral already completed or expired"));
    }

    @Test
    void testGetReferral_Success() {
        var createRequest = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            id.payu.promotion.domain.Referral.RewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        var response = given()
            .contentType(ContentType.JSON)
            .body(createRequest)
            .post()
            .jsonPath()
            .getString("id");

        given()
            .when()
            .get("/" + response)
            .then()
            .statusCode(200)
            .body("id", notNullValue())
            .body("referrerAccountId", equalTo(REFERRER_ACCOUNT_ID))
            .body("status", equalTo("PENDING"));
    }

    @Test
    void testGetReferral_NotFound_Returns404() {
        UUID nonExistentId = UUID.randomUUID();

        given()
            .when()
            .get("/" + nonExistentId)
            .then()
            .statusCode(404)
            .body("message", equalTo("Referral not found"));
    }

    @Test
    void testGetReferralByCode_Success() {
        var createRequest = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            id.payu.promotion.domain.Referral.RewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        var referralCode = given()
            .contentType(ContentType.JSON)
            .body(createRequest)
            .post()
            .jsonPath()
            .getString("referralCode");

        given()
            .when()
            .get("/code/" + referralCode)
            .then()
            .statusCode(200)
            .body("referralCode", equalTo(referralCode))
            .body("referrerAccountId", equalTo(REFERRER_ACCOUNT_ID))
            .body("status", equalTo("PENDING"));
    }

    @Test
    void testGetReferralByCode_NotFound_Returns404() {
        given()
            .when()
            .get("/code/NONEXISTENT")
            .then()
            .statusCode(404)
            .body("message", equalTo("Referral code not found"));
    }

    @Test
    void testGetReferralsByReferrer_Success() {
        var request1 = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            id.payu.promotion.domain.Referral.RewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        var request2 = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            id.payu.promotion.domain.Referral.RewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        given()
            .contentType(ContentType.JSON)
            .body(request1)
            .post();

        given()
            .contentType(ContentType.JSON)
            .body(request2)
            .post();

        given()
            .when()
            .get("/referrer/" + REFERRER_ACCOUNT_ID)
            .then()
            .statusCode(200)
            .body("", hasSize(2))
            .body("referrerAccountId", everyItem(equalTo(REFERRER_ACCOUNT_ID)));
    }

    @Test
    void testGetReferralSummary_Success() {
        var request1 = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            id.payu.promotion.domain.Referral.RewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        var request2 = new CreateReferralRequest(
            REFERRER_ACCOUNT_ID,
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            id.payu.promotion.domain.Referral.RewardType.CASHBACK,
            LocalDateTime.now().plusMonths(3)
        );

        var referralCode = given()
            .contentType(ContentType.JSON)
            .body(request1)
            .post()
            .jsonPath()
            .getString("referralCode");

        given()
            .contentType(ContentType.JSON)
            .body(request2)
            .post();

        var completeRequest = new CompleteReferralRequest(
            referralCode,
            REFEREE_ACCOUNT_ID
        );

        given()
            .contentType(ContentType.JSON)
            .body(completeRequest)
            .post("/complete");

        given()
            .when()
            .get("/referrer/" + REFERRER_ACCOUNT_ID + "/summary")
            .then()
            .statusCode(200)
            .body("totalReferrals", equalTo(2))
            .body("completedReferrals", equalTo(1))
            .body("pendingReferrals", equalTo(1))
            .body("referralCode", notNullValue());
    }

    @Test
    void testGetReferralSummary_NoReferrals() {
        given()
            .when()
            .get("/referrer/non-existent/summary")
            .then()
            .statusCode(200)
            .body("referralCode", nullValue())
            .body("totalReferrals", equalTo(0))
            .body("completedReferrals", equalTo(0))
            .body("pendingReferrals", equalTo(0));
    }
}
