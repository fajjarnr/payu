package id.payu.backoffice.resource;

import id.payu.backoffice.domain.CustomerCase;
import id.payu.backoffice.domain.FraudCase;
import id.payu.backoffice.domain.KycReview;
import id.payu.backoffice.dto.*;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BackofficeResourceTest {

    @Test
    @Order(1)
    public void testCreateKycReview() {
        KycReviewRequest request = new KycReviewRequest(
                "user-123",
                "DOC-001",
                "PASSPORT",
                "123456789",
                "http://example.com/doc.jpg",
                "John Doe",
                "123 Main St",
                "+1234567890",
                "Initial request"
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/backoffice/kyc-reviews")
                .then()
                .statusCode(201)
                .body("userId", equalTo("user-123"))
                .body("status", equalTo("PENDING"));
    }

    @Test
    @Order(2)
    public void testListKycReviews() {
        given()
                .when()
                .get("/api/v1/backoffice/kyc-reviews")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0));
    }

    @Test
    @Order(3)
    public void testReviewKyc() {
        // First get the ID of the pending review
        String id = given()
                .queryParam("status", "PENDING")
                .when()
                .get("/api/v1/backoffice/kyc-reviews")
                .then()
                .statusCode(200)
                .extract()
                .path("[0].id");

        KycReviewDecisionRequest request = new KycReviewDecisionRequest(
                KycReviewDecisionRequest.KycReviewStatus.APPROVED,
                "Looks good"
        );

        given()
                .contentType(ContentType.JSON)
                .header("X-Admin-User", "admin1")
                .body(request)
                .when()
                .post("/api/v1/backoffice/kyc-reviews/" + id + "/review")
                .then()
                .statusCode(200)
                .body("status", equalTo("APPROVED"))
                .body("reviewedBy", equalTo("admin1"));
    }

    @Test
    @Order(4)
    public void testCreateFraudCase() {
        given()
                .contentType(ContentType.URLENC)
                .formParam("userId", "fraud-user-1")
                .formParam("accountNumber", "ACC-FRAUD-1")
                .formParam("transactionType", "TRANSFER")
                .formParam("amount", 1000.0)
                .formParam("fraudType", "PHISHING")
                .formParam("riskLevel", "HIGH")
                .formParam("description", "Suspicious activity")
                .formParam("evidence", "{\"file\": \"logs.txt\"}")
                .when()
                .post("/api/v1/backoffice/fraud-cases")
                .then()
                .statusCode(201)
                .body("userId", equalTo("fraud-user-1"))
                .body("riskLevel", equalTo("HIGH"));
    }

    @Test
    @Order(5)
    public void testCreateCustomerCase() {
        CustomerCaseRequest request = new CustomerCaseRequest(
                "customer-1",
                "ACC-CUST-1",
                CustomerCase.CaseType.ACCOUNT_ISSUE,
                CustomerCase.Priority.HIGH,
                "Login issue",
                "Cannot login",
                "Please check"
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/backoffice/customer-cases")
                .then()
                .statusCode(201)
                .body("userId", equalTo("customer-1"))
                .body("priority", equalTo("HIGH"));
    }
}
