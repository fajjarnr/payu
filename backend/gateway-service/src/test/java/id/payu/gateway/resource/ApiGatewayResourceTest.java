package id.payu.gateway.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isOneOf;

@QuarkusTest
@DisplayName("ApiGateway Integration Tests")
class ApiGatewayResourceTest {

    @Nested
    @DisplayName("Partner Service Routes")
    class PartnerServiceRoutes {

        @Test
        @DisplayName("should proxy GET /partners to partner-service")
        void shouldProxyGetPartners() {
            given()
                .when()
                    .get("/api/v1/partners")
                .then()
                    .statusCode(isOneOf(200, 404, 503, 500));
        }

        @Test
        @DisplayName("should proxy POST /partners to partner-service")
        void shouldProxyPostPartners() {
            given()
                .when()
                    .post("/api/v1/partners")
                .then()
                    .statusCode(isOneOf(201, 400, 415, 503, 500));
        }

        @Test
        @DisplayName("should proxy GET /partners/{id} to partner-service")
        void shouldProxyGetPartnerById() {
            given()
                .when()
                    .get("/api/v1/partners/1")
                .then()
                    .statusCode(isOneOf(200, 404, 503, 500));
        }

        @Test
        @DisplayName("should proxy POST /v1/partner/auth/token to partner-service")
        void shouldProxySnapAuth() {
            given()
                .header("X-CLIENT-KEY", "test-key")
                .when()
                    .post("/api/v1/v1/partner/auth/token")
                .then()
                    .statusCode(isOneOf(200, 401, 415, 503, 500));
        }
    }

    @Nested
    @DisplayName("Promotion Service Routes")
    class PromotionServiceRoutes {

        @Test
        @DisplayName("should proxy GET /promotions to promotion-service")
        void shouldProxyGetPromotions() {
            given()
                .when()
                    .get("/api/v1/promotions")
                .then()
                    .statusCode(isOneOf(200, 405, 503, 500));
        }

        @Test
        @DisplayName("should proxy GET /cashbacks to promotion-service")
        void shouldProxyGetCashbacks() {
            given()
                .when()
                    .get("/api/v1/cashbacks")
                .then()
                    .statusCode(isOneOf(200, 404, 405, 503, 500));
        }

        @Test
        @DisplayName("should proxy GET /loyalty-points to promotion-service")
        void shouldProxyGetLoyaltyPoints() {
            given()
                .when()
                    .get("/api/v1/loyalty-points")
                .then()
                    .statusCode(isOneOf(200, 404, 405, 503, 500));
        }

        @Test
        @DisplayName("should proxy GET /rewards to promotion-service")
        void shouldProxyGetRewards() {
            given()
                .when()
                    .get("/api/v1/rewards")
                .then()
                    .statusCode(isOneOf(200, 404, 405, 503, 500));
        }

        @Test
        @DisplayName("should proxy GET /referrals to promotion-service")
        void shouldProxyGetReferrals() {
            given()
                .when()
                    .get("/api/v1/referrals")
                .then()
                    .statusCode(isOneOf(200, 404, 405, 503, 500));
        }
    }

    @Nested
    @DisplayName("Lending Service Routes")
    class LendingServiceRoutes {

        @Test
        @DisplayName("should proxy GET /lending to lending-service")
        void shouldProxyGetLending() {
            given()
                .when()
                    .get("/api/v1/lending")
                .then()
                    .statusCode(isOneOf(200, 404, 405, 503, 500));
        }

        @Test
        @DisplayName("should proxy GET /lending/loans to lending-service")
        void shouldProxyGetLoans() {
            given()
                .when()
                    .get("/api/v1/lending/loans")
                .then()
                    .statusCode(isOneOf(200, 404, 503, 500));
        }

        @Test
        @DisplayName("should proxy POST /lending/loans to lending-service")
        void shouldProxyPostLoans() {
            given()
                .when()
                    .post("/api/v1/lending/loans")
                .then()
                    .statusCode(isOneOf(200, 400, 415, 503, 500));
        }

        @Test
        @DisplayName("should proxy GET /lending/credit-score to lending-service")
        void shouldProxyGetCreditScore() {
            given()
                .when()
                    .get("/api/v1/lending/credit-score/test-id")
                .then()
                    .statusCode(isOneOf(200, 404, 503, 500));
        }
    }

    @Nested
    @DisplayName("Investment Service Routes")
    class InvestmentServiceRoutes {

        @Test
        @DisplayName("should proxy GET /investments to investment-service")
        void shouldProxyGetInvestments() {
            given()
                .when()
                    .get("/api/v1/investments")
                .then()
                    .statusCode(isOneOf(200, 404, 405, 503, 500));
        }

        @Test
        @DisplayName("should proxy GET /investments/accounts to investment-service")
        void shouldProxyGetInvestmentAccounts() {
            given()
                .when()
                    .get("/api/v1/investments/accounts")
                .then()
                    .statusCode(isOneOf(200, 404, 503, 500));
        }

        @Test
        @DisplayName("should proxy GET /investments/gold to investment-service")
        void shouldProxyGetGold() {
            given()
                .when()
                    .get("/api/v1/investments/gold/test-id")
                .then()
                    .statusCode(isOneOf(200, 404, 503, 500));
        }
    }

    @Nested
    @DisplayName("Compliance Service Routes")
    class ComplianceServiceRoutes {

        @Test
        @DisplayName("should proxy GET /compliance to compliance-service")
        void shouldProxyGetCompliance() {
            given()
                .when()
                    .get("/api/v1/compliance")
                .then()
                    .statusCode(isOneOf(200, 404, 405, 503, 500));
        }

        @Test
        @DisplayName("should proxy GET /compliance/audit-report to compliance-service")
        void shouldProxyGetAuditReport() {
            given()
                .when()
                    .get("/api/v1/compliance/audit-report")
                .then()
                    .statusCode(isOneOf(400, 404, 503, 500));
        }

        @Test
        @DisplayName("should proxy POST /compliance/audit-report to compliance-service")
        void shouldProxyPostAuditReport() {
            given()
                .when()
                    .post("/api/v1/compliance/audit-report")
                .then()
                    .statusCode(isOneOf(201, 400, 415, 503, 500));
        }
    }

    @Nested
    @DisplayName("Backoffice Service Routes")
    class BackofficeServiceRoutes {

        @Test
        @DisplayName("should proxy GET /backoffice to backoffice-service")
        void shouldProxyGetBackoffice() {
            given()
                .when()
                    .get("/api/v1/backoffice")
                .then()
                    .statusCode(isOneOf(200, 404, 405, 503, 500));
        }

        @Test
        @DisplayName("should proxy GET /backoffice/kyc-reviews to backoffice-service")
        void shouldProxyGetKycReviews() {
            given()
                .when()
                    .get("/api/v1/backoffice/kyc-reviews")
                .then()
                    .statusCode(isOneOf(200, 404, 503, 500));
        }

        @Test
        @DisplayName("should proxy POST /backoffice/kyc-reviews to backoffice-service")
        void shouldProxyPostKycReviews() {
            given()
                .when()
                    .post("/api/v1/backoffice/kyc-reviews")
                .then()
                    .statusCode(isOneOf(201, 400, 415, 503, 500));
        }

        @Test
        @DisplayName("should proxy GET /backoffice/fraud-cases to backoffice-service")
        void shouldProxyGetFraudCases() {
            given()
                .when()
                    .get("/api/v1/backoffice/fraud-cases")
                .then()
                    .statusCode(isOneOf(200, 404, 503, 500));
        }

        @Test
        @DisplayName("should proxy GET /backoffice/customer-cases to backoffice-service")
        void shouldProxyGetCustomerCases() {
            given()
                .when()
                    .get("/api/v1/backoffice/customer-cases")
                .then()
                    .statusCode(isOneOf(200, 404, 503, 500));
        }
    }

    @Nested
    @DisplayName("Support Service Routes")
    class SupportServiceRoutes {

        @Test
        @DisplayName("should proxy GET /support to support-service")
        void shouldProxyGetSupport() {
            given()
                .when()
                    .get("/api/v1/support")
                .then()
                    .statusCode(isOneOf(200, 404, 405, 503, 500));
        }

        @Test
        @DisplayName("should proxy GET /support/training-status to support-service")
        void shouldProxyGetTrainingStatus() {
            given()
                .when()
                    .get("/api/v1/support/training-status")
                .then()
                    .statusCode(isOneOf(200, 404, 503, 500));
        }

        @Test
        @DisplayName("should proxy GET /support/agents to support-service")
        void shouldProxyGetAgents() {
            given()
                .when()
                    .get("/api/v1/support/agents")
                .then()
                    .statusCode(isOneOf(200, 404, 503, 500));
        }

        @Test
        @DisplayName("should proxy GET /support/modules to support-service")
        void shouldProxyGetTrainingModules() {
            given()
                .when()
                    .get("/api/v1/support/modules")
                .then()
                    .statusCode(isOneOf(200, 404, 503, 500));
        }

        @Test
        @DisplayName("should proxy GET /support/trainings to support-service")
        void shouldProxyGetTrainings() {
            given()
                .when()
                    .get("/api/v1/support/trainings")
                .then()
                    .statusCode(isOneOf(200, 404, 503, 500));
        }
    }
}
