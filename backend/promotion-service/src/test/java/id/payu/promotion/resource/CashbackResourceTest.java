package id.payu.promotion.resource;

import id.payu.promotion.dto.CreateCashbackRequest;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@Disabled("Resource tests require Docker/Testcontainers - disabled when Docker not available")
@TestHTTPEndpoint(CashbackResource.class)
class CashbackResourceTest {

    private static final String TEST_ACCOUNT_ID = "acc-test-456";

    @BeforeEach
    void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void testCreateCashback_Success() {
        var request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            "txn-001",
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "GROCERY",
            "PROMO2024"
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("accountId", equalTo(TEST_ACCOUNT_ID))
            .body("transactionId", equalTo("txn-001"))
            .body("transactionAmount", equalTo(1000.00f))
            .body("cashbackAmount", equalTo(20.00f))
            .body("percentage", equalTo(2.0000f))
            .body("merchantCode", equalTo("MERCHANT001"))
            .body("categoryCode", equalTo("GROCERY"))
            .body("cashbackCode", equalTo("PROMO2024"))
            .body("status", equalTo("CREDITED"));
    }

    @Test
    void testCreateCashback_DiningCategory_3Percent() {
        var request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            "txn-002",
            new BigDecimal("500.00"),
            "MERCHANT002",
            "DINING",
            null
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("cashbackAmount", equalTo(15.00f))
            .body("percentage", equalTo(3.0000f))
            .body("status", equalTo("CREDITED"));
    }

    @Test
    void testCreateCashback_InvalidRequest_Returns400() {
        var request = new CreateCashbackRequest(
            null,
            "txn-003",
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "GROCERY",
            null
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
    void testGetCashback_Success() {
        var request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            "txn-001",
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "GROCERY",
            null
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
            .body("cashbackAmount", equalTo(20.00f));
    }

    @Test
    void testGetCashback_NotFound_Returns404() {
        UUID nonExistentId = UUID.randomUUID();

        given()
            .when()
            .get("/" + nonExistentId)
            .then()
            .statusCode(404)
            .body("message", equalTo("Cashback not found"));
    }

    @Test
    void testGetCashbacksByAccount_Success() {
        var request1 = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            "txn-001",
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "GROCERY",
            null
        );

        var request2 = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            "txn-002",
            new BigDecimal("500.00"),
            "MERCHANT002",
            "DINING",
            null
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
    void testGetCashbackSummary_Success() {
        var request1 = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            "txn-001",
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "GROCERY",
            null
        );

        var request2 = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            "txn-002",
            new BigDecimal("500.00"),
            "MERCHANT002",
            "DINING",
            null
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
            .get("/account/" + TEST_ACCOUNT_ID + "/summary")
            .then()
            .statusCode(200)
            .body("totalCashback", equalTo(35.00f))
            .body("pendingCashback", equalTo(0.0f))
            .body("creditedCashback", equalTo(35.00f))
            .body("transactionCount", equalTo(2));
    }

    @Test
    void testGetCashbackSummary_NoTransactions() {
        given()
            .when()
            .get("/account/non-existent/summary")
            .then()
            .statusCode(200)
            .body("totalCashback", equalTo(0.0f))
            .body("pendingCashback", equalTo(0.0f))
            .body("creditedCashback", equalTo(0.0f))
            .body("transactionCount", equalTo(0));
    }
}
