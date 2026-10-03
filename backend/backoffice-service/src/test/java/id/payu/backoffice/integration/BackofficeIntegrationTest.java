package id.payu.backoffice.integration;

import id.payu.backoffice.domain.CustomerCase;
import id.payu.backoffice.domain.FraudCase;
import id.payu.backoffice.domain.KycReview;
import id.payu.backoffice.domain.CaseType;
import id.payu.backoffice.domain.CustomerCaseStatus;
import id.payu.backoffice.domain.FraudCaseStatus;
import id.payu.backoffice.domain.KycStatus;
import id.payu.backoffice.domain.Priority;
import id.payu.backoffice.domain.RiskLevel;
import id.payu.backoffice.interfaces.dto.CustomerCaseRequest;
import id.payu.backoffice.interfaces.dto.FraudCaseDecisionRequest;
import id.payu.backoffice.interfaces.dto.KycReviewDecisionRequest;
import id.payu.backoffice.interfaces.dto.KycReviewRequest;
import id.payu.backoffice.application.service.CustomerCaseService;
import id.payu.backoffice.application.service.FraudCaseService;
import id.payu.backoffice.application.service.KycReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Backoffice Service, run against an in-memory H2
 * database initialized by Flyway.
 */
@SpringBootTest
@ActiveProfiles("integrationtest")
@DisplayName("Backoffice Service Integration Tests")
class BackofficeIntegrationTest {

    @Autowired
    KycReviewService kycReviewService;

    @Autowired
    FraudCaseService fraudCaseService;

    @Autowired
    CustomerCaseService customerCaseService;

    @Test
    @DisplayName("Should create and retrieve KYC review from database")
    @Transactional
    void shouldCreateAndRetrieveKycReview() {
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        KycReviewRequest request = new KycReviewRequest(
            testUserId,
            testAccountNumber,
            "PASSPORT",
            "A1234567",
            "http://example.com/doc.jpg",
            "John Doe",
            "123 Main St, Jakarta",
            "+628123456789",
            "Initial KYC submission"
        );

        KycReview createdReview = kycReviewService.create(request);
        Optional<KycReview> retrievedReview = kycReviewService.getById(createdReview.getId());

        assertTrue(retrievedReview.isPresent());
        assertEquals(createdReview.getId(), retrievedReview.get().getId());
        assertEquals(testUserId, retrievedReview.get().getUserId());
        assertEquals(testAccountNumber, retrievedReview.get().getAccountNumber());
        assertEquals("PASSPORT", retrievedReview.get().getDocumentType());
        assertEquals(KycStatus.PENDING, retrievedReview.get().getStatus());
        assertNotNull(retrievedReview.get().getCreatedAt());

        kycReviewService.delete(createdReview.getId());
    }

    @Test
    @DisplayName("Should approve KYC review and update audit fields")
    @Transactional
    void shouldApproveKycReview() {
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        KycReviewRequest request = new KycReviewRequest(
            testUserId,
            testAccountNumber,
            "KTP",
            "3201234567890001",
            "http://example.com/ktp.jpg",
            "Jane Doe",
            "456 Oak Ave, Surabaya",
            "+628987654321",
            "KYC for verification"
        );

        KycReview review = kycReviewService.create(request);
        KycReviewDecisionRequest decisionRequest = new KycReviewDecisionRequest(
            id.payu.backoffice.interfaces.dto.KycReviewStatus.APPROVED,
            "Documents verified, identity confirmed"
        );

        KycReview result = kycReviewService.review(review.getId(), decisionRequest, "admin1");

        assertEquals(KycStatus.APPROVED, result.getStatus());
        assertEquals("Documents verified, identity confirmed", result.getNotes());
        assertEquals("admin1", result.getReviewedBy());
        assertNotNull(result.getReviewedAt());
        assertNotNull(result.getCreatedAt());

        kycReviewService.delete(review.getId());
    }

    @Test
    @DisplayName("Should reject KYC review with reason")
    @Transactional
    void shouldRejectKycReview() {
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        KycReviewRequest request = new KycReviewRequest(
            testUserId,
            testAccountNumber,
            "SIM",
            "1234567890123456",
            "http://example.com/sim.jpg",
            "Test User",
            "Test Address",
            "+628111111111",
            "KYC submission"
        );

        KycReview review = kycReviewService.create(request);
        KycReviewDecisionRequest decisionRequest = new KycReviewDecisionRequest(
            id.payu.backoffice.interfaces.dto.KycReviewStatus.REJECTED,
            "Document expired, please submit valid ID"
        );

        KycReview result = kycReviewService.review(review.getId(), decisionRequest, "admin2");

        assertEquals(KycStatus.REJECTED, result.getStatus());
        assertTrue(result.getNotes().contains("expired"));
        assertEquals("admin2", result.getReviewedBy());

        kycReviewService.delete(review.getId());
    }

    @Test
    @DisplayName("Should retrieve KYC reviews by status")
    @Transactional
    void shouldRetrieveKycReviewsByStatus() {
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        kycReviewService.create(new KycReviewRequest(
            testUserId + "-1", testAccountNumber + "-1", "KTP", "111", null, null, null, null, "Pending 1"
        ));
        kycReviewService.create(new KycReviewRequest(
            testUserId + "-2", testAccountNumber + "-2", "KTP", "222", null, null, null, null, "Pending 2"
        ));

        List<KycReview> pendingReviews = kycReviewService.listByStatus(KycStatus.PENDING, 0, 10);

        assertNotNull(pendingReviews);
        assertTrue(pendingReviews.stream().anyMatch(r -> r.getUserId().startsWith(testUserId)));
        assertTrue(pendingReviews.stream().allMatch(r -> r.getStatus() == KycStatus.PENDING));

        pendingReviews.stream()
            .filter(r -> r.getUserId().startsWith(testUserId))
            .forEach(r -> kycReviewService.delete(r.getId()));
    }

    @Test
    @DisplayName("Should create and retrieve fraud case from database")
    @Transactional
    void shouldCreateAndRetrieveFraudCase() {
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();
        UUID transactionId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("15000000");

        FraudCase createdCase = fraudCaseService.create(
            testUserId,
            testAccountNumber,
            transactionId,
            "TRANSFER",
            amount,
            "ACCOUNT_TAKEOVER",
            RiskLevel.HIGH,
            "Unusual login pattern followed by large transfer",
            "{\"ip\": \"192.168.1.100\", \"device\": \"unknown\"}"
        );

        Optional<FraudCase> retrievedCase = fraudCaseService.getById(createdCase.getId());

        assertTrue(retrievedCase.isPresent());
        assertEquals(createdCase.getId(), retrievedCase.get().getId());
        assertEquals(testUserId, retrievedCase.get().getUserId());
        assertEquals(transactionId, retrievedCase.get().getTransactionId());
        assertEquals("TRANSFER", retrievedCase.get().getTransactionType());
        assertEquals(amount, retrievedCase.get().getAmount());
        assertEquals(RiskLevel.HIGH, retrievedCase.get().getRiskLevel());
        assertEquals(FraudCaseStatus.OPEN, retrievedCase.get().getStatus());

        fraudCaseService.delete(createdCase.getId());
    }

    @Test
    @DisplayName("Should assign fraud case to investigator")
    @Transactional
    void shouldAssignFraudCase() {
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        FraudCase fraudCase = fraudCaseService.create(
            testUserId,
            testAccountNumber,
            UUID.randomUUID(),
            "PAYMENT",
            new BigDecimal("5000000"),
            "CARD_FRAUD",
            RiskLevel.MEDIUM,
            "Multiple failed card attempts",
            null
        );

        FraudCase assignedCase = fraudCaseService.assign(fraudCase.getId(), "investigator1");

        assertEquals("investigator1", assignedCase.getAssignedTo());
        assertEquals(FraudCaseStatus.UNDER_INVESTIGATION, assignedCase.getStatus());

        fraudCaseService.delete(fraudCase.getId());
    }

    @Test
    @DisplayName("Should resolve fraud case as confirmed fraud")
    @Transactional
    void shouldResolveFraudCaseAsConfirmed() {
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        FraudCase fraudCase = fraudCaseService.create(
            testUserId,
            testAccountNumber,
            UUID.randomUUID(),
            "TRANSFER",
            new BigDecimal("10000000"),
            "PHISHING",
            RiskLevel.CRITICAL,
            "Customer reports phishing attack",
            "{\"email\": \"scam@fake.com\"}"
        );

        fraudCaseService.assign(fraudCase.getId(), "investigator2");

        FraudCaseDecisionRequest decisionRequest = new FraudCaseDecisionRequest(
            id.payu.backoffice.interfaces.dto.FraudCaseStatus.RESOLVED,
            "Confirmed phishing attack. Account credentials compromised. Action taken."
        );

        FraudCase resolvedCase = fraudCaseService.resolve(fraudCase.getId(), decisionRequest, "investigator2");

        assertEquals(FraudCaseStatus.RESOLVED, resolvedCase.getStatus());
        assertEquals("investigator2", resolvedCase.getResolvedBy());
        assertNotNull(resolvedCase.getResolvedAt());

        fraudCaseService.delete(fraudCase.getId());
    }

    @Test
    @DisplayName("Should retrieve fraud cases by risk level")
    @Transactional
    void shouldRetrieveFraudCasesByRiskLevel() {
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        fraudCaseService.create(
            testUserId + "-1", testAccountNumber + "-1", UUID.randomUUID(), "TRANSFER",
            new BigDecimal("5000000"), "FRAUD_A", RiskLevel.HIGH,
            "High risk case", null
        );

        fraudCaseService.create(
            testUserId + "-2", testAccountNumber + "-2", UUID.randomUUID(), "PAYMENT",
            new BigDecimal("3000000"), "FRAUD_B", RiskLevel.HIGH,
            "Another high risk case", null
        );

        List<FraudCase> highRiskCases = fraudCaseService.listByRiskLevel(RiskLevel.HIGH, 0, 10);

        assertNotNull(highRiskCases);
        assertTrue(highRiskCases.stream().anyMatch(c -> c.getUserId().startsWith(testUserId)));
        assertTrue(highRiskCases.stream().filter(c -> c.getUserId().startsWith(testUserId))
            .allMatch(c -> c.getRiskLevel() == RiskLevel.HIGH));

        highRiskCases.stream()
            .filter(c -> c.getUserId().startsWith(testUserId))
            .forEach(c -> fraudCaseService.delete(c.getId()));
    }

    @Test
    @DisplayName("Should create and retrieve customer case from database")
    @Transactional
    void shouldCreateAndRetrieveCustomerCase() {
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        CustomerCaseRequest request = new CustomerCaseRequest(
            testUserId,
            testAccountNumber,
            CaseType.TRANSACTION_DISPUTE,
            Priority.HIGH,
            "Unauthorized transaction on my account",
            "I see a transaction I didn't make",
            "Customer called support"
        );

        CustomerCase createdCase = customerCaseService.create(request);
        Optional<CustomerCase> retrievedCase = customerCaseService.getById(createdCase.getId());

        assertTrue(retrievedCase.isPresent());
        assertEquals(createdCase.getId(), retrievedCase.get().getId());
        assertEquals(testUserId, retrievedCase.get().getUserId());
        assertEquals(CaseType.TRANSACTION_DISPUTE, retrievedCase.get().getCaseType());
        assertEquals(Priority.HIGH, retrievedCase.get().getPriority());
        assertEquals(CustomerCaseStatus.OPEN, retrievedCase.get().getStatus());
        assertNotNull(retrievedCase.get().getCaseNumber());

        customerCaseService.delete(createdCase.getId());
    }

    @Test
    @DisplayName("Should assign customer case to agent")
    @Transactional
    void shouldAssignCustomerCase() {
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        CustomerCaseRequest request = new CustomerCaseRequest(
            testUserId,
            testAccountNumber,
            CaseType.ACCOUNT_ISSUE,
            Priority.MEDIUM,
            "Cannot access account",
            "Login fails with correct credentials",
            null
        );

        CustomerCase customerCase = customerCaseService.create(request);

        CustomerCase assignedCase = customerCaseService.assign(customerCase.getId(), "agent1");

        assertEquals("agent1", assignedCase.getAssignedTo());
        assertEquals(CustomerCaseStatus.IN_PROGRESS, assignedCase.getStatus());

        customerCaseService.delete(customerCase.getId());
    }

    @Test
    @DisplayName("Should update and resolve customer case")
    @Transactional
    void shouldUpdateAndResolveCustomerCase() {
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        CustomerCaseRequest request = new CustomerCaseRequest(
            testUserId,
            testAccountNumber,
            CaseType.TECHNICAL_ISSUE,
            Priority.LOW,
            "App not loading",
            "Mobile app crashes on startup",
            null
        );

        CustomerCase customerCase = customerCaseService.create(request);
        customerCaseService.assign(customerCase.getId(), "support1");

        CustomerCase updatedCase = customerCaseService.update(
            customerCase.getId(),
            new id.payu.backoffice.interfaces.dto.CustomerCaseUpdateRequest(
                CustomerCaseStatus.RESOLVED,
                "Fixed in version 2.1. Please update app"
            ),
            "support1"
        );

        assertEquals(CustomerCaseStatus.RESOLVED, updatedCase.getStatus());
        assertEquals("support1", updatedCase.getResolvedBy());
        assertNotNull(updatedCase.getResolvedAt());
        assertTrue(updatedCase.getNotes().contains("version 2.1"));

        customerCaseService.delete(customerCase.getId());
    }

    @Test
    @DisplayName("Should retrieve customer cases by priority")
    @Transactional
    void shouldRetrieveCustomerCasesByPriority() {
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        customerCaseService.create(new CustomerCaseRequest(
            testUserId + "-1", testAccountNumber + "-1",
            CaseType.TRANSACTION_DISPUTE, Priority.URGENT,
            "Urgent case 1", "Description 1", null
        ));

        customerCaseService.create(new CustomerCaseRequest(
            testUserId + "-2", testAccountNumber + "-2",
            CaseType.ACCOUNT_ISSUE, Priority.URGENT,
            "Urgent case 2", "Description 2", null
        ));

        List<CustomerCase> urgentCases = customerCaseService.listByPriority(Priority.URGENT, 0, 10);

        assertNotNull(urgentCases);
        assertTrue(urgentCases.stream().anyMatch(c -> c.getUserId().startsWith(testUserId)));
        assertTrue(urgentCases.stream().filter(c -> c.getUserId().startsWith(testUserId))
            .allMatch(c -> c.getPriority() == Priority.URGENT));

        urgentCases.stream()
            .filter(c -> c.getUserId().startsWith(testUserId))
            .forEach(c -> customerCaseService.delete(c.getId()));
    }

    @Test
    @DisplayName("Should maintain audit trail for KYC review operations")
    @Transactional
    void shouldMaintainAuditTrailForKycOperations() {
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        KycReviewRequest request = new KycReviewRequest(
            testUserId, testAccountNumber, "KTP", "123", null, "User", null, null, "Initial"
        );

        KycReview review = kycReviewService.create(request);

        KycReviewDecisionRequest decisionRequest = new KycReviewDecisionRequest(
            id.payu.backoffice.interfaces.dto.KycReviewStatus.APPROVED,
            "Approved after verification"
        );
        KycReview updatedReview = kycReviewService.review(review.getId(), decisionRequest, "admin_audit");

        assertNotNull(updatedReview.getCreatedAt());
        assertNotNull(updatedReview.getReviewedAt());
        assertEquals("admin_audit", updatedReview.getReviewedBy());

        kycReviewService.delete(review.getId());
    }

    @Test
    @DisplayName("Should maintain audit trail for fraud case operations")
    @Transactional
    void shouldMaintainAuditTrailForFraudOperations() {
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        FraudCase fraudCase = fraudCaseService.create(
            testUserId, testAccountNumber, UUID.randomUUID(), "TRANSFER",
            new BigDecimal("5000000"), "FRAUD", RiskLevel.HIGH,
            "Audit test", null
        );

        fraudCaseService.assign(fraudCase.getId(), "investigator_audit");

        FraudCaseDecisionRequest decisionRequest = new FraudCaseDecisionRequest(
            id.payu.backoffice.interfaces.dto.FraudCaseStatus.CLOSED,
            "Case closed after investigation"
        );
        FraudCase resolvedCase = fraudCaseService.resolve(fraudCase.getId(), decisionRequest, "investigator_audit");

        assertNotNull(resolvedCase.getCreatedAt());
        assertEquals("investigator_audit", resolvedCase.getAssignedTo());
        assertEquals("investigator_audit", resolvedCase.getResolvedBy());
        assertNotNull(resolvedCase.getResolvedAt());

        fraudCaseService.delete(fraudCase.getId());
    }

    @Test
    @DisplayName("Should maintain audit trail for customer case operations")
    @Transactional
    void shouldMaintainAuditTrailForCustomerOperations() {
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        CustomerCaseRequest request = new CustomerCaseRequest(
            testUserId, testAccountNumber, CaseType.ACCOUNT_ISSUE,
            Priority.HIGH, "Audit test", "Audit description", null
        );

        CustomerCase customerCase = customerCaseService.create(request);

        customerCaseService.assign(customerCase.getId(), "agent_audit");
        CustomerCase updatedCase = customerCaseService.update(
            customerCase.getId(),
            new id.payu.backoffice.interfaces.dto.CustomerCaseUpdateRequest(
                CustomerCaseStatus.RESOLVED,
                "Issue resolved"
            ),
            "agent_audit"
        );

        assertNotNull(updatedCase.getCreatedAt());
        assertEquals("agent_audit", updatedCase.getAssignedTo());
        assertEquals("agent_audit", updatedCase.getResolvedBy());
        assertNotNull(updatedCase.getResolvedAt());

        customerCaseService.delete(customerCase.getId());
    }

    @Test
    @DisplayName("Should throw exception when reviewing non-existent KYC review")
    void shouldThrowExceptionWhenReviewingNonExistentKyc() {
        KycReviewDecisionRequest decisionRequest = new KycReviewDecisionRequest(
            id.payu.backoffice.interfaces.dto.KycReviewStatus.APPROVED,
            "Test"
        );

        assertThrows(IllegalArgumentException.class, () -> {
            kycReviewService.review(UUID.randomUUID(), decisionRequest, "admin1");
        });
    }

    @Test
    @DisplayName("Should throw exception when assigning non-existent fraud case")
    void shouldThrowExceptionWhenAssigningNonExistentFraudCase() {
        assertThrows(IllegalArgumentException.class, () -> {
            fraudCaseService.assign(UUID.randomUUID(), "investigator1");
        });
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent customer case")
    void shouldThrowExceptionWhenUpdatingNonExistentCustomerCase() {
        id.payu.backoffice.interfaces.dto.CustomerCaseUpdateRequest updateRequest =
            new id.payu.backoffice.interfaces.dto.CustomerCaseUpdateRequest(
                CustomerCaseStatus.IN_PROGRESS,
                "Test update"
            );

        assertThrows(IllegalArgumentException.class, () -> {
            customerCaseService.update(UUID.randomUUID(), updateRequest, "agent1");
        });
    }

    @Test
    @DisplayName("Should handle complete KYC review workflow from creation to approval")
    @Transactional
    void shouldHandleCompleteKycWorkflow() {
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        KycReviewRequest request = new KycReviewRequest(
            testUserId, testAccountNumber, "PASSPORT", "P123456",
            "http://example.com/passport.jpg", "Workflow User",
            "Jakarta, Indonesia", "+628123456789", "Complete workflow test"
        );

        KycReview review = kycReviewService.create(request);
        assertEquals(KycStatus.PENDING, review.getStatus());

        KycReviewDecisionRequest infoRequest = new KycReviewDecisionRequest(
            id.payu.backoffice.interfaces.dto.KycReviewStatus.REQUIRES_ADDITIONAL_INFO,
            "Please provide proof of address"
        );
        review = kycReviewService.review(review.getId(), infoRequest, "admin1");
        assertEquals(KycStatus.REQUIRES_ADDITIONAL_INFO, review.getStatus());

        KycReviewDecisionRequest approvalRequest = new KycReviewDecisionRequest(
            id.payu.backoffice.interfaces.dto.KycReviewStatus.APPROVED,
            "All documents verified and approved"
        );
        review = kycReviewService.review(review.getId(), approvalRequest, "admin2");

        assertEquals(KycStatus.APPROVED, review.getStatus());
        assertEquals("admin2", review.getReviewedBy());
        assertNotNull(review.getReviewedAt());

        kycReviewService.delete(review.getId());
    }

    @Test
    @DisplayName("Should handle complete fraud case workflow from detection to resolution")
    @Transactional
    void shouldHandleCompleteFraudWorkflow() {
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        FraudCase fraudCase = fraudCaseService.create(
            testUserId, testAccountNumber, UUID.randomUUID(), "TRANSFER",
            new BigDecimal("25000000"), "MONEY_LAUNDERING",
            RiskLevel.CRITICAL,
            "Suspicious large transaction pattern detected",
            "{\"pattern\": \"layering\", \"alerts\": 5}"
        );

        assertEquals(FraudCaseStatus.OPEN, fraudCase.getStatus());

        fraudCase = fraudCaseService.assign(fraudCase.getId(), "senior_investigator");
        assertEquals(FraudCaseStatus.UNDER_INVESTIGATION, fraudCase.getStatus());

        FraudCaseDecisionRequest escalateRequest = new FraudCaseDecisionRequest(
            id.payu.backoffice.interfaces.dto.FraudCaseStatus.ESCALATED,
            "Complex case requiring compliance team review"
        );
        fraudCase = fraudCaseService.resolve(fraudCase.getId(), escalateRequest, "senior_investigator");
        assertEquals(FraudCaseStatus.ESCALATED, fraudCase.getStatus());

        FraudCaseDecisionRequest resolveRequest = new FraudCaseDecisionRequest(
            id.payu.backoffice.interfaces.dto.FraudCaseStatus.CLOSED,
            "Case reviewed and closed. SAR filed."
        );
        fraudCase = fraudCaseService.resolve(fraudCase.getId(), resolveRequest, "compliance_officer");

        assertEquals(FraudCaseStatus.CLOSED, fraudCase.getStatus());
        assertEquals("compliance_officer", fraudCase.getResolvedBy());
        assertNotNull(fraudCase.getResolvedAt());

        fraudCaseService.delete(fraudCase.getId());
    }

    @Test
    @DisplayName("Should handle complete customer case workflow from creation to closure")
    @Transactional
    void shouldHandleCompleteCustomerCaseWorkflow() {
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        CustomerCaseRequest request = new CustomerCaseRequest(
            testUserId, testAccountNumber, CaseType.TRANSACTION_DISPUTE,
            Priority.URGENT,
            "Unauthorized transaction of IDR 10.000.000",
            "I never made this transaction",
            "Customer called hotline, very distressed"
        );

        CustomerCase customerCase = customerCaseService.create(request);
        assertEquals(CustomerCaseStatus.OPEN, customerCase.getStatus());
        assertEquals(Priority.URGENT, customerCase.getPriority());

        customerCase = customerCaseService.assign(customerCase.getId(), "senior_agent");
        assertEquals(CustomerCaseStatus.IN_PROGRESS, customerCase.getStatus());

        customerCase = customerCaseService.update(
            customerCase.getId(),
            new id.payu.backoffice.interfaces.dto.CustomerCaseUpdateRequest(
                CustomerCaseStatus.IN_PROGRESS,
                "Investigating with payment processor. Evidence gathered."
            ),
            "senior_agent"
        );

        customerCase = customerCaseService.update(
            customerCase.getId(),
            new id.payu.backoffice.interfaces.dto.CustomerCaseUpdateRequest(
                CustomerCaseStatus.RESOLVED,
                "Confirmed unauthorized. Refund processed. Case closed."
            ),
            "senior_agent"
        );

        assertEquals(CustomerCaseStatus.RESOLVED, customerCase.getStatus());
        assertEquals("senior_agent", customerCase.getResolvedBy());
        assertNotNull(customerCase.getResolvedAt());

        customerCaseService.delete(customerCase.getId());
    }

    @Test
    @DisplayName("Should retrieve paginated KYC reviews for dashboard")
    @Transactional
    void shouldRetrievePaginatedKycReviewsForDashboard() {
        String testUserId = "test-user-dash-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-dash-" + System.currentTimeMillis();

        for (int i = 0; i < 3; i++) {
            kycReviewService.create(new KycReviewRequest(
                testUserId + "-dash-" + i,
                testAccountNumber + "-dash-" + i,
                "KTP", "123" + i, null, "User " + i, null, null, "Dashboard test " + i
            ));
        }

        List<KycReview> page1 = kycReviewService.listAll(0, 10);

        assertNotNull(page1);
        assertTrue(page1.stream().anyMatch(r -> r.getUserId().startsWith(testUserId)));

        page1.stream()
            .filter(r -> r.getUserId().startsWith(testUserId))
            .forEach(r -> kycReviewService.delete(r.getId()));
    }

    @Test
    @DisplayName("Should retrieve paginated fraud cases for dashboard")
    @Transactional
    void shouldRetrievePaginatedFraudCasesForDashboard() {
        String testUserId = "test-user-dash-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-dash-" + System.currentTimeMillis();

        for (int i = 0; i < 3; i++) {
            fraudCaseService.create(
                testUserId + "-dash-" + i,
                testAccountNumber + "-dash-" + i,
                UUID.randomUUID(),
                "TRANSFER",
                new BigDecimal(1000000 * (i + 1)),
                "FRAUD_TYPE_" + i,
                RiskLevel.MEDIUM,
                "Dashboard fraud test " + i,
                null
            );
        }

        List<FraudCase> page1 = fraudCaseService.listAll(0, 10);

        assertNotNull(page1);
        assertTrue(page1.stream().anyMatch(c -> c.getUserId().startsWith(testUserId)));

        page1.stream()
            .filter(c -> c.getUserId().startsWith(testUserId))
            .forEach(c -> fraudCaseService.delete(c.getId()));
    }

    @Test
    @DisplayName("Should retrieve paginated customer cases for dashboard")
    @Transactional
    void shouldRetrievePaginatedCustomerCasesForDashboard() {
        String testUserId = "test-user-dash-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-dash-" + System.currentTimeMillis();

        for (int i = 0; i < 3; i++) {
            customerCaseService.create(new CustomerCaseRequest(
                testUserId + "-dash-" + i,
                testAccountNumber + "-dash-" + i,
                CaseType.GENERAL_INQUIRY,
                Priority.MEDIUM,
                "Dashboard subject " + i,
                "Dashboard description " + i,
                null
            ));
        }

        List<CustomerCase> page1 = customerCaseService.listAll(0, 10);

        assertNotNull(page1);
        assertTrue(page1.stream().anyMatch(c -> c.getUserId().startsWith(testUserId)));

        page1.stream()
            .filter(c -> c.getUserId().startsWith(testUserId))
            .forEach(c -> customerCaseService.delete(c.getId()));
    }
}
