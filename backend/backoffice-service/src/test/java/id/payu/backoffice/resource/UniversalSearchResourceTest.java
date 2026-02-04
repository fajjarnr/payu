package id.payu.backoffice.resource;

import id.payu.backoffice.domain.CustomerCase;
import id.payu.backoffice.domain.FraudCase;
import id.payu.backoffice.domain.KycReview;
import id.payu.backoffice.repository.CustomerCaseRepository;
import id.payu.backoffice.repository.FraudCaseRepository;
import id.payu.backoffice.repository.KycReviewRepository;
import id.payu.backoffice.testutil.IntegrationTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integrationtest")
@IntegrationTest
@Disabled("Resource tests require Docker + full dependencies")
class UniversalSearchResourceTest {

    @LocalServerPort
    private int port;

    @Autowired
    KycReviewRepository kycReviewRepository;

    @Autowired
    FraudCaseRepository fraudCaseRepository;

    @Autowired
    CustomerCaseRepository customerCaseRepository;

    private UUID kycReviewId;
    private UUID fraudCaseId;
    private UUID customerCaseId;

    @BeforeEach
    @Transactional
    void setUp() {
        RestAssured.port = port;
        KycReview kycReview = new KycReview();
        kycReview.userId = "searchUser123";
        kycReview.accountNumber = "SEARCHACC123";
        kycReview.documentType = "KTP";
        kycReview.documentNumber = "3209999999990001";
        kycReview.fullName = "Jane Doe";
        kycReview.address = "Test Address";
        kycReview.phoneNumber = "08987654321";
        kycReview.status = KycReview.KycStatus.PENDING;
        kycReview.createdAt = LocalDateTime.now();
        kycReviewRepository.save(kycReview);
        kycReviewId = kycReview.getId();

        FraudCase fraudCase = new FraudCase();
        fraudCase.userId = "searchUser123";
        fraudCase.accountNumber = "SEARCHACC123";
        fraudCase.transactionId = UUID.randomUUID();
        fraudCase.transactionType = "TRANSFER";
        fraudCase.amount = new BigDecimal("5000000");
        fraudCase.fraudType = "Suspicious Activity";
        fraudCase.riskLevel = FraudCase.RiskLevel.MEDIUM;
        fraudCase.status = FraudCase.CaseStatus.OPEN;
        fraudCase.description = "Suspicious transaction pattern";
        fraudCase.createdAt = LocalDateTime.now();
        fraudCaseRepository.save(fraudCase);
        fraudCaseId = fraudCase.getId();

        CustomerCase customerCase = new CustomerCase();
        customerCase.userId = "searchUser456";
        customerCase.accountNumber = "SEARCHACC456";
        customerCase.caseNumber = "CASE-SEARCH-123";
        customerCase.caseType = CustomerCase.CaseType.ACCOUNT_ISSUE;
        customerCase.priority = CustomerCase.Priority.MEDIUM;
        customerCase.subject = "Account locked";
        customerCase.description = "Unable to access account";
        customerCase.status = CustomerCase.CaseStatus.OPEN;
        customerCase.createdAt = LocalDateTime.now();
        customerCaseRepository.save(customerCase);
        customerCaseId = customerCase.getId();
    }

    @Test
    void testSearchWithPostRequest() {
        given()
                .contentType("application/json")
                .body("{\"query\": \"searchUser123\"}")
                .when()
                .post("/api/v1/backoffice/search")
                .then()
                .statusCode(200)
                .body("query", equalTo("searchUser123"))
                .body("totalResults", greaterThanOrEqualTo(2))
                .body("results", not(empty()));
    }

    @Test
    void testSearchWithGetRequest() {
        given()
                .queryParam("q", "searchUser123")
                .when()
                .get("/api/v1/backoffice/search")
                .then()
                .statusCode(200)
                .body("query", equalTo("searchUser123"))
                .body("totalResults", greaterThanOrEqualTo(2))
                .body("results", not(empty()));
    }

    @Test
    void testSearchWithEntityTypeFilter() {
        given()
                .queryParam("q", "searchUser123")
                .queryParam("type", "kyc")
                .when()
                .get("/api/v1/backoffice/search")
                .then()
                .statusCode(200)
                .body("query", equalTo("searchUser123"))
                .body("results", not(empty()))
                .body("results.every { it.type == 'kyc' }", is(true));
    }

    @Test
    void testSearchWithPagination() {
        given()
                .queryParam("q", "searchUser")
                .queryParam("page", 0)
                .queryParam("size", 1)
                .when()
                .get("/api/v1/backoffice/search")
                .then()
                .statusCode(200)
                .body("page", equalTo(0))
                .body("size", equalTo(1))
                .body("results.size()", equalTo(1));
    }

    @Test
    void testSearchByAccountNumber() {
        given()
                .queryParam("q", "SEARCHACC123")
                .when()
                .get("/api/v1/backoffice/search")
                .then()
                .statusCode(200)
                .body("totalResults", greaterThanOrEqualTo(2))
                .body("results.type", hasItem(equalTo("kyc")))
                .body("results.type", hasItem(equalTo("fraud")));
    }

    @Test
    void testSearchByDocumentNumber() {
        given()
                .queryParam("q", "3209999999990001")
                .when()
                .get("/api/v1/backoffice/search")
                .then()
                .statusCode(200)
                .body("totalResults", greaterThanOrEqualTo(1))
                .body("results", hasItem(hasEntry("type", "kyc")));
    }

    @Test
    void testSearchByCaseNumber() {
        given()
                .queryParam("q", "CASE-SEARCH-123")
                .when()
                .get("/api/v1/backoffice/search")
                .then()
                .statusCode(200)
                .body("totalResults", greaterThanOrEqualTo(1))
                .body("results", hasItem(hasEntry("type", "customer")));
    }

    @Test
    void testSearchNoResults() {
        given()
                .queryParam("q", "nonexistent987654321")
                .when()
                .get("/api/v1/backoffice/search")
                .then()
                .statusCode(200)
                .body("totalResults", equalTo(0))
                .body("results", empty());
    }

    @Test
    void testSearchWithoutQueryParameter() {
        given()
                .when()
                .get("/api/v1/backoffice/search")
                .then()
                .statusCode(400);
    }

    @Test
    void testSearchResultStructure() {
        given()
                .queryParam("q", "searchUser123")
                .when()
                .get("/api/v1/backoffice/search")
                .then()
                .statusCode(200)
                .body("results[0]", hasKey("type"))
                .body("results[0]", hasKey("id"))
                .body("results[0]", hasKey("title"))
                .body("results[0]", hasKey("description"))
                .body("results[0]", hasKey("userId"))
                .body("results[0]", hasKey("accountNumber"))
                .body("results[0]", hasKey("status"))
                .body("results[0]", hasKey("createdAt"))
                .body("results[0]", hasKey("details"));
    }

    @Test
    void testSearchPostWithInvalidPagination() {
        given()
                .contentType("application/json")
                .body("{\"query\": \"searchUser123\", \"page\": -1, \"size\": 200}")
                .when()
                .post("/api/v1/backoffice/search")
                .then()
                .statusCode(200)
                .body("page", equalTo(0))
                .body("size", equalTo(20));
    }
}
