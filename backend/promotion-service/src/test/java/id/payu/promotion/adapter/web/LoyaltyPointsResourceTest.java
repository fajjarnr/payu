package id.payu.promotion.adapter.web;

import id.payu.promotion.dto.CreateLoyaltyPointsRequest;
import id.payu.promotion.dto.RedeemLoyaltyPointsRequest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import id.payu.promotion.adapter.persistence.repository.LoyaltyPointsRepository;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.UUID;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LoyaltyPointsResourceTest {

    @org.springframework.boot.test.web.server.LocalServerPort
    int port;

    @Autowired
    LoyaltyPointsRepository loyaltyPointsRepository;

    private static final String TEST_ACCOUNT_ID = "test-user";

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1/loyalty-points";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        loyaltyPointsRepository.deleteAll();
    }

    @Test
    void testAddPoints_Success() {
        var request = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            "txn-001",
            id.payu.promotion.domain.LoyaltyPoints.TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("accountId", equalTo(TEST_ACCOUNT_ID))
            .body("points", equalTo(100))
            .body("balanceAfter", equalTo(100))
            .body("transactionType", equalTo("EARNED"));
    }

    @Test
    void testAddPoints_InvalidRequest_Returns400() {
        var request = new CreateLoyaltyPointsRequest(
            null,
            "txn-001",
            id.payu.promotion.domain.LoyaltyPoints.TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
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
    void testRedeemPoints_Success() {
        var earnRequest = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            "txn-earn-001",
            id.payu.promotion.domain.LoyaltyPoints.TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        );

        given()
            .contentType(ContentType.JSON)
            .body(earnRequest)
            .post();

        var redeemRequest = new RedeemLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            50,
            "txn-redeem-001"
        );

        given()
            .contentType(ContentType.JSON)
            .body(redeemRequest)
            .when()
            .post("/redeem")
            .then()
            .statusCode(200)
            .body("accountId", equalTo(TEST_ACCOUNT_ID))
            .body("points", equalTo(-50))
            .body("balanceAfter", equalTo(50))
            .body("transactionType", equalTo("REDEEMED"));
    }

    @Test
    void testRedeemPoints_InsufficientBalance_Returns400() {
        var redeemRequest = new RedeemLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            1000,
            "txn-redeem-001"
        );

        given()
            .contentType(ContentType.JSON)
            .body(redeemRequest)
            .when()
            .post("/redeem")
            .then()
            .statusCode(400)
            .body("message", containsString("Insufficient loyalty points balance"));
    }

    @Test
    void testGetLoyaltyPoints_Success() {
        var request = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            "txn-001",
            id.payu.promotion.domain.LoyaltyPoints.TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        );

        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .post()
            .jsonPath()
            .getString("id");

        given()
            .when()
            .get("/" + response)
            .then()
            .statusCode(200)
            .body("accountId", equalTo(TEST_ACCOUNT_ID))
            .body("points", equalTo(100));
    }

    @Test
    void testGetLoyaltyPoints_NotFound_Returns404() {
        UUID nonExistentId = UUID.randomUUID();

        given()
            .when()
            .get("/" + nonExistentId)
            .then()
            .statusCode(404)
            .body("message", equalTo("Loyalty points record not found"));
    }

    @Test
    void testGetLoyaltyPointsByAccount_Success() {
        var request1 = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            "txn-001",
            id.payu.promotion.domain.LoyaltyPoints.TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        );

        var request2 = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            "txn-002",
            id.payu.promotion.domain.LoyaltyPoints.TransactionType.EARNED,
            50,
            LocalDateTime.now().plusMonths(6)
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
            .get("/account/" + TEST_ACCOUNT_ID)
            .then()
            .statusCode(200)
            .body("", hasSize(2))
            .body("accountId", everyItem(equalTo(TEST_ACCOUNT_ID)));
    }

    @Test
    void testGetBalance_Success() {
        var request = new CreateLoyaltyPointsRequest(
            TEST_ACCOUNT_ID,
            "txn-001",
            id.payu.promotion.domain.LoyaltyPoints.TransactionType.EARNED,
            100,
            LocalDateTime.now().plusMonths(6)
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .post();

        given()
            .when()
            .get("/account/" + TEST_ACCOUNT_ID + "/balance")
            .then()
            .statusCode(200)
            .body("currentBalance", equalTo(100))
            .body("totalEarned", equalTo(100))
            .body("totalRedeemed", equalTo(0))
            .body("expiredPoints", equalTo(0));
    }

    @Test
    void testGetBalance_NoTransactions_ReturnsZero() {
        given()
            .when()
            .get("/account/test-user/balance")
            .then()
            .statusCode(200)
            .body("currentBalance", equalTo(0))
            .body("totalEarned", equalTo(0))
            .body("totalRedeemed", equalTo(0))
            .body("expiredPoints", equalTo(0));
    }
}
