package id.payu.backoffice.integration;

import id.payu.backoffice.domain.CustomerCase;
import id.payu.backoffice.domain.FraudCase;
import id.payu.backoffice.domain.KycReview;
import id.payu.backoffice.dto.CustomerCaseRequest;
import id.payu.backoffice.dto.FraudCaseDecisionRequest;
import id.payu.backoffice.dto.KycReviewDecisionRequest;
import id.payu.backoffice.dto.KycReviewRequest;
import id.payu.backoffice.service.CustomerCaseService;
import id.payu.backoffice.service.FraudCaseService;
import id.payu.backoffice.service.KycReviewService;
import id.payu.backoffice.testutil.PostgreSQLResourceTestLifecycleManager;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Backoffice Service.
 *
 * These tests verify the complete backoffice functionality against the database,
 * ensuring proper:
 *
 * 1. KYC review lifecycle (creation, approval, rejection, additional info requests)
 * 2. Fraud case management (creation, assignment, investigation, resolution)
 * 3. Customer case operations (creation, assignment, updates, resolution)
 * 4. Audit trail for admin operations
 * 5. Query operations across different statuses and priorities
 *
 * Uses @Tag("integration") to allow selective test execution.
 *
 * Tests require Docker to be running with: mvn test -Ddocker.enabled=true
 *
 * @author PayU Backend Team
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "docker.enabled", matches = "true")
@DisplayName("Backoffice Service Integration Tests")
@Tag("integration")
class BackofficeIntegrationTest {

    @Inject
    KycReviewService kycReviewService;

    @Inject
    FraudCaseService fraudCaseService;

    @Inject
    CustomerCaseService customerCaseService;

    // ===== KYC Review Integration Tests =====

    @Test
    @DisplayName("Should create and retrieve KYC review from database")
    @Transactional
    void shouldCreateAndRetrieveKycReview() {
        // Given
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

        // When
        KycReview createdReview = kycReviewService.create(request);
        Optional<KycReview> retrievedReview = kycReviewService.getById(createdReview.id);

        // Then
        assertTrue(retrievedReview.isPresent());
        assertEquals(createdReview.id, retrievedReview.get().id);
        assertEquals(testUserId, retrievedReview.get().userId);
        assertEquals(testAccountNumber, retrievedReview.get().accountNumber);
        assertEquals("PASSPORT", retrievedReview.get().documentType);
        assertEquals(KycReview.KycStatus.PENDING, retrievedReview.get().status);
        assertNotNull(retrievedReview.get().createdAt);

        // Cleanup
        kycReviewService.delete(createdReview.id);
    }

    @Test
    @DisplayName("Should approve KYC review and update audit fields")
    @Transactional
    void shouldApproveKycReview() {
        // Given
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
            KycReviewDecisionRequest.KycReviewStatus.APPROVED,
            "Documents verified, identity confirmed"
        );

        // When
        KycReview result = kycReviewService.review(review.id, decisionRequest, "admin1");

        // Then
        assertEquals(KycReview.KycStatus.APPROVED, result.status);
        assertEquals("Documents verified, identity confirmed", result.notes);
        assertEquals("admin1", result.reviewedBy);
        assertNotNull(result.reviewedAt);
        assertNotNull(result.createdAt);

        // Cleanup
        kycReviewService.delete(review.id);
    }

    @Test
    @DisplayName("Should reject KYC review with reason")
    @Transactional
    void shouldRejectKycReview() {
        // Given
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
            KycReviewDecisionRequest.KycReviewStatus.REJECTED,
            "Document expired, please submit valid ID"
        );

        // When
        KycReview result = kycReviewService.review(review.id, decisionRequest, "admin2");

        // Then
        assertEquals(KycReview.KycStatus.REJECTED, result.status);
        assertTrue(result.notes.contains("expired"));
        assertEquals("admin2", result.reviewedBy);

        // Cleanup
        kycReviewService.delete(review.id);
    }

    @Test
    @DisplayName("Should retrieve KYC reviews by status")
    @Transactional
    void shouldRetrieveKycReviewsByStatus() {
        // Given
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        kycReviewService.create(new KycReviewRequest(
            testUserId + "-1", testAccountNumber + "-1", "KTP", "111", null, null, null, null, "Pending 1"
        ));
        kycReviewService.create(new KycReviewRequest(
            testUserId + "-2", testAccountNumber + "-2", "KTP", "222", null, null, null, null, "Pending 2"
        ));

        // When
        List<KycReview> pendingReviews = kycReviewService.listByStatus(KycReview.KycStatus.PENDING, 0, 10);

        // Then
        assertNotNull(pendingReviews);
        assertTrue(pendingReviews.stream().anyMatch(r -> r.userId.startsWith(testUserId)));
        assertTrue(pendingReviews.stream().allMatch(r -> r.status == KycReview.KycStatus.PENDING));

        // Cleanup
        pendingReviews.stream()
            .filter(r -> r.userId.startsWith(testUserId))
            .forEach(r -> kycReviewService.delete(r.id));
    }

    // ===== Fraud Case Integration Tests =====

    @Test
    @DisplayName("Should create and retrieve fraud case from database")
    @Transactional
    void shouldCreateAndRetrieveFraudCase() {
        // Given
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();
        UUID transactionId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("15000000");

        // When
        FraudCase createdCase = fraudCaseService.create(
            testUserId,
            testAccountNumber,
            transactionId,
            "TRANSFER",
            amount,
            "ACCOUNT_TAKEOVER",
            FraudCase.RiskLevel.HIGH,
            "Unusual login pattern followed by large transfer",
            "{\"ip\": \"192.168.1.100\", \"device\": \"unknown\"}"
        );

        Optional<FraudCase> retrievedCase = fraudCaseService.getById(createdCase.id);

        // Then
        assertTrue(retrievedCase.isPresent());
        assertEquals(createdCase.id, retrievedCase.get().id);
        assertEquals(testUserId, retrievedCase.get().userId);
        assertEquals(transactionId, retrievedCase.get().transactionId);
        assertEquals("TRANSFER", retrievedCase.get().transactionType);
        assertEquals(amount, retrievedCase.get().amount);
        assertEquals(FraudCase.RiskLevel.HIGH, retrievedCase.get().riskLevel);
        assertEquals(FraudCase.CaseStatus.OPEN, retrievedCase.get().status);

        // Cleanup
        fraudCaseService.delete(createdCase.id);
    }

    @Test
    @DisplayName("Should assign fraud case to investigator")
    @Transactional
    void shouldAssignFraudCase() {
        // Given
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        FraudCase fraudCase = fraudCaseService.create(
            testUserId,
            testAccountNumber,
            UUID.randomUUID(),
            "PAYMENT",
            new BigDecimal("5000000"),
            "CARD_FRAUD",
            FraudCase.RiskLevel.MEDIUM,
            "Multiple failed card attempts",
            null
        );

        // When
        FraudCase assignedCase = fraudCaseService.assign(fraudCase.id, "investigator1");

        // Then
        assertEquals("investigator1", assignedCase.assignedTo);
        assertEquals(FraudCase.CaseStatus.UNDER_INVESTIGATION, assignedCase.status);

        // Cleanup
        fraudCaseService.delete(fraudCase.id);
    }

    @Test
    @DisplayName("Should resolve fraud case as confirmed fraud")
    @Transactional
    void shouldResolveFraudCaseAsConfirmed() {
        // Given
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        FraudCase fraudCase = fraudCaseService.create(
            testUserId,
            testAccountNumber,
            UUID.randomUUID(),
            "TRANSFER",
            new BigDecimal("10000000"),
            "PHISHING",
            FraudCase.RiskLevel.CRITICAL,
            "Customer reports phishing attack",
            "{\"email\": \"scam@fake.com\"}"
        );

        fraudCaseService.assign(fraudCase.id, "investigator2");

        FraudCaseDecisionRequest decisionRequest = new FraudCaseDecisionRequest(
            FraudCaseDecisionRequest.FraudCaseStatus.RESOLVED,
            "Confirmed phishing attack. Account credentials compromised. Action taken."
        );

        // When
        FraudCase resolvedCase = fraudCaseService.resolve(fraudCase.id, decisionRequest, "investigator2");

        // Then
        assertEquals(FraudCase.CaseStatus.RESOLVED, resolvedCase.status);
        assertEquals("investigator2", resolvedCase.resolvedBy);
        assertNotNull(resolvedCase.resolvedAt);

        // Cleanup
        fraudCaseService.delete(fraudCase.id);
    }

    @Test
    @DisplayName("Should retrieve fraud cases by risk level")
    @Transactional
    void shouldRetrieveFraudCasesByRiskLevel() {
        // Given
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        fraudCaseService.create(
            testUserId + "-1", testAccountNumber + "-1", UUID.randomUUID(), "TRANSFER",
            new BigDecimal("5000000"), "FRAUD_A", FraudCase.RiskLevel.HIGH,
            "High risk case", null
        );

        fraudCaseService.create(
            testUserId + "-2", testAccountNumber + "-2", UUID.randomUUID(), "PAYMENT",
            new BigDecimal("3000000"), "FRAUD_B", FraudCase.RiskLevel.HIGH,
            "Another high risk case", null
        );

        // When
        List<FraudCase> highRiskCases = fraudCaseService.listByRiskLevel(FraudCase.RiskLevel.HIGH, 0, 10);

        // Then
        assertNotNull(highRiskCases);
        assertTrue(highRiskCases.stream().anyMatch(c -> c.userId.startsWith(testUserId)));
        assertTrue(highRiskCases.stream().filter(c -> c.userId.startsWith(testUserId))
            .allMatch(c -> c.riskLevel == FraudCase.RiskLevel.HIGH));

        // Cleanup
        highRiskCases.stream()
            .filter(c -> c.userId.startsWith(testUserId))
            .forEach(c -> fraudCaseService.delete(c.id));
    }

    // ===== Customer Case Integration Tests =====

    @Test
    @DisplayName("Should create and retrieve customer case from database")
    @Transactional
    void shouldCreateAndRetrieveCustomerCase() {
        // Given
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        CustomerCaseRequest request = new CustomerCaseRequest(
            testUserId,
            testAccountNumber,
            CustomerCase.CaseType.TRANSACTION_DISPUTE,
            CustomerCase.Priority.HIGH,
            "Unauthorized transaction on my account",
            "I see a transaction I didn't make",
            "Customer called support"
        );

        // When
        CustomerCase createdCase = customerCaseService.create(request);
        Optional<CustomerCase> retrievedCase = customerCaseService.getById(createdCase.id);

        // Then
        assertTrue(retrievedCase.isPresent());
        assertEquals(createdCase.id, retrievedCase.get().id);
        assertEquals(testUserId, retrievedCase.get().userId);
        assertEquals(CustomerCase.CaseType.TRANSACTION_DISPUTE, retrievedCase.get().caseType);
        assertEquals(CustomerCase.Priority.HIGH, retrievedCase.get().priority);
        assertEquals(CustomerCase.CaseStatus.OPEN, retrievedCase.get().status);
        assertNotNull(retrievedCase.get().caseNumber);

        // Cleanup
        customerCaseService.delete(createdCase.id);
    }

    @Test
    @DisplayName("Should assign customer case to agent")
    @Transactional
    void shouldAssignCustomerCase() {
        // Given
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        CustomerCaseRequest request = new CustomerCaseRequest(
            testUserId,
            testAccountNumber,
            CustomerCase.CaseType.ACCOUNT_ISSUE,
            CustomerCase.Priority.MEDIUM,
            "Cannot access account",
            "Login fails with correct credentials",
            null
        );

        CustomerCase customerCase = customerCaseService.create(request);

        // When
        CustomerCase assignedCase = customerCaseService.assign(customerCase.id, "agent1");

        // Then
        assertEquals("agent1", assignedCase.assignedTo);
        assertEquals(CustomerCase.CaseStatus.IN_PROGRESS, assignedCase.status);

        // Cleanup
        customerCaseService.delete(customerCase.id);
    }

    @Test
    @DisplayName("Should update and resolve customer case")
    @Transactional
    void shouldUpdateAndResolveCustomerCase() {
        // Given
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        CustomerCaseRequest request = new CustomerCaseRequest(
            testUserId,
            testAccountNumber,
            CustomerCase.CaseType.TECHNICAL_ISSUE,
            CustomerCase.Priority.LOW,
            "App not loading",
            "Mobile app crashes on startup",
            null
        );

        CustomerCase customerCase = customerCaseService.create(request);
        customerCaseService.assign(customerCase.id, "support1");

        // When
        CustomerCase updatedCase = customerCaseService.update(
            customerCase.id,
            new id.payu.backoffice.dto.CustomerCaseUpdateRequest(
                CustomerCase.CaseStatus.RESOLVED,
                "Fixed in version 2.1. Please update app"
            ),
            "support1"
        );

        // Then
        assertEquals(CustomerCase.CaseStatus.RESOLVED, updatedCase.status);
        assertEquals("support1", updatedCase.resolvedBy);
        assertNotNull(updatedCase.resolvedAt);
        assertTrue(updatedCase.notes.contains("version 2.1"));

        // Cleanup
        customerCaseService.delete(customerCase.id);
    }

    @Test
    @DisplayName("Should retrieve customer cases by priority")
    @Transactional
    void shouldRetrieveCustomerCasesByPriority() {
        // Given
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        customerCaseService.create(new CustomerCaseRequest(
            testUserId + "-1", testAccountNumber + "-1",
            CustomerCase.CaseType.TRANSACTION_DISPUTE, CustomerCase.Priority.URGENT,
            "Urgent case 1", "Description 1", null
        ));

        customerCaseService.create(new CustomerCaseRequest(
            testUserId + "-2", testAccountNumber + "-2",
            CustomerCase.CaseType.ACCOUNT_ISSUE, CustomerCase.Priority.URGENT,
            "Urgent case 2", "Description 2", null
        ));

        // When
        List<CustomerCase> urgentCases = customerCaseService.listByPriority(CustomerCase.Priority.URGENT, 0, 10);

        // Then
        assertNotNull(urgentCases);
        assertTrue(urgentCases.stream().anyMatch(c -> c.userId.startsWith(testUserId)));
        assertTrue(urgentCases.stream().filter(c -> c.userId.startsWith(testUserId))
            .allMatch(c -> c.priority == CustomerCase.Priority.URGENT));

        // Cleanup
        urgentCases.stream()
            .filter(c -> c.userId.startsWith(testUserId))
            .forEach(c -> customerCaseService.delete(c.id));
    }

    // ===== Audit Trail Tests =====

    @Test
    @DisplayName("Should maintain audit trail for KYC review operations")
    @Transactional
    void shouldMaintainAuditTrailForKycOperations() {
        // Given
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        KycReviewRequest request = new KycReviewRequest(
            testUserId, testAccountNumber, "KTP", "123", null, "User", null, null, "Initial"
        );

        KycReview review = kycReviewService.create(request);

        // When
        KycReviewDecisionRequest decisionRequest = new KycReviewDecisionRequest(
            KycReviewDecisionRequest.KycReviewStatus.APPROVED,
            "Approved after verification"
        );
        KycReview updatedReview = kycReviewService.review(review.id, decisionRequest, "admin_audit");

        // Then - Verify audit trail is maintained
        assertNotNull(updatedReview.createdAt);
        assertNotNull(updatedReview.reviewedAt);
        assertEquals("admin_audit", updatedReview.reviewedBy);

        // Cleanup
        kycReviewService.delete(review.id);
    }

    @Test
    @DisplayName("Should maintain audit trail for fraud case operations")
    @Transactional
    void shouldMaintainAuditTrailForFraudOperations() {
        // Given
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        FraudCase fraudCase = fraudCaseService.create(
            testUserId, testAccountNumber, UUID.randomUUID(), "TRANSFER",
            new BigDecimal("5000000"), "FRAUD", FraudCase.RiskLevel.HIGH,
            "Audit test", null
        );

        // When
        fraudCaseService.assign(fraudCase.id, "investigator_audit");

        FraudCaseDecisionRequest decisionRequest = new FraudCaseDecisionRequest(
            FraudCaseDecisionRequest.FraudCaseStatus.CLOSED,
            "Case closed after investigation"
        );
        FraudCase resolvedCase = fraudCaseService.resolve(fraudCase.id, decisionRequest, "investigator_audit");

        // Then - Verify audit trail
        assertNotNull(resolvedCase.createdAt);
        assertEquals("investigator_audit", resolvedCase.assignedTo);
        assertEquals("investigator_audit", resolvedCase.resolvedBy);
        assertNotNull(resolvedCase.resolvedAt);

        // Cleanup
        fraudCaseService.delete(fraudCase.id);
    }

    @Test
    @DisplayName("Should maintain audit trail for customer case operations")
    @Transactional
    void shouldMaintainAuditTrailForCustomerOperations() {
        // Given
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        CustomerCaseRequest request = new CustomerCaseRequest(
            testUserId, testAccountNumber, CustomerCase.CaseType.ACCOUNT_ISSUE,
            CustomerCase.Priority.HIGH, "Audit test", "Audit description", null
        );

        CustomerCase customerCase = customerCaseService.create(request);

        // When
        customerCaseService.assign(customerCase.id, "agent_audit");
        CustomerCase updatedCase = customerCaseService.update(
            customerCase.id,
            new id.payu.backoffice.dto.CustomerCaseUpdateRequest(
                CustomerCase.CaseStatus.RESOLVED,
                "Issue resolved"
            ),
            "agent_audit"
        );

        // Then - Verify audit trail
        assertNotNull(updatedCase.createdAt);
        assertEquals("agent_audit", updatedCase.assignedTo);
        assertEquals("agent_audit", updatedCase.resolvedBy);
        assertNotNull(updatedCase.resolvedAt);

        // Cleanup
        customerCaseService.delete(customerCase.id);
    }

    // ===== Error Handling Tests =====

    @Test
    @DisplayName("Should throw exception when reviewing non-existent KYC review")
    void shouldThrowExceptionWhenReviewingNonExistentKyc() {
        // Given
        KycReviewDecisionRequest decisionRequest = new KycReviewDecisionRequest(
            KycReviewDecisionRequest.KycReviewStatus.APPROVED,
            "Test"
        );

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            kycReviewService.review(UUID.randomUUID(), decisionRequest, "admin1");
        });
    }

    @Test
    @DisplayName("Should throw exception when assigning non-existent fraud case")
    void shouldThrowExceptionWhenAssigningNonExistentFraudCase() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            fraudCaseService.assign(UUID.randomUUID(), "investigator1");
        });
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent customer case")
    void shouldThrowExceptionWhenUpdatingNonExistentCustomerCase() {
        // Given
        id.payu.backoffice.dto.CustomerCaseUpdateRequest updateRequest =
            new id.payu.backoffice.dto.CustomerCaseUpdateRequest(
                CustomerCase.CaseStatus.IN_PROGRESS,
                "Test update"
            );

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            customerCaseService.update(UUID.randomUUID(), updateRequest, "agent1");
        });
    }

    // ===== Complex Workflow Tests =====

    @Test
    @DisplayName("Should handle complete KYC review workflow from creation to approval")
    @Transactional
    void shouldHandleCompleteKycWorkflow() {
        // Given
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        KycReviewRequest request = new KycReviewRequest(
            testUserId, testAccountNumber, "PASSPORT", "P123456",
            "http://example.com/passport.jpg", "Workflow User",
            "Jakarta, Indonesia", "+628123456789", "Complete workflow test"
        );

        // When - Create review
        KycReview review = kycReviewService.create(request);
        assertEquals(KycReview.KycStatus.PENDING, review.status);

        // When - Request additional info
        KycReviewDecisionRequest infoRequest = new KycReviewDecisionRequest(
            KycReviewDecisionRequest.KycReviewStatus.REQUIRES_ADDITIONAL_INFO,
            "Please provide proof of address"
        );
        review = kycReviewService.review(review.id, infoRequest, "admin1");
        assertEquals(KycReview.KycStatus.REQUIRES_ADDITIONAL_INFO, review.status);

        // When - Final approval
        KycReviewDecisionRequest approvalRequest = new KycReviewDecisionRequest(
            KycReviewDecisionRequest.KycReviewStatus.APPROVED,
            "All documents verified and approved"
        );
        review = kycReviewService.review(review.id, approvalRequest, "admin2");

        // Then - Verify final state
        assertEquals(KycReview.KycStatus.APPROVED, review.status);
        assertEquals("admin2", review.reviewedBy);
        assertNotNull(review.reviewedAt);

        // Cleanup
        kycReviewService.delete(review.id);
    }

    @Test
    @DisplayName("Should handle complete fraud case workflow from detection to resolution")
    @Transactional
    void shouldHandleCompleteFraudWorkflow() {
        // Given
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        FraudCase fraudCase = fraudCaseService.create(
            testUserId, testAccountNumber, UUID.randomUUID(), "TRANSFER",
            new BigDecimal("25000000"), "MONEY_LAUNDERING",
            FraudCase.RiskLevel.CRITICAL,
            "Suspicious large transaction pattern detected",
            "{\"pattern\": \"layering\", \"alerts\": 5}"
        );

        assertEquals(FraudCase.CaseStatus.OPEN, fraudCase.status);

        // When - Assign to investigator
        fraudCase = fraudCaseService.assign(fraudCase.id, "senior_investigator");
        assertEquals(FraudCase.CaseStatus.UNDER_INVESTIGATION, fraudCase.status);

        // When - Escalate for further review
        FraudCaseDecisionRequest escalateRequest = new FraudCaseDecisionRequest(
            FraudCaseDecisionRequest.FraudCaseStatus.ESCALATED,
            "Complex case requiring compliance team review"
        );
        fraudCase = fraudCaseService.resolve(fraudCase.id, escalateRequest, "senior_investigator");
        assertEquals(FraudCase.CaseStatus.ESCALATED, fraudCase.status);

        // When - Final resolution
        FraudCaseDecisionRequest resolveRequest = new FraudCaseDecisionRequest(
            FraudCaseDecisionRequest.FraudCaseStatus.CLOSED,
            "Case reviewed and closed. SAR filed."
        );
        fraudCase = fraudCaseService.resolve(fraudCase.id, resolveRequest, "compliance_officer");

        // Then - Verify final state
        assertEquals(FraudCase.CaseStatus.CLOSED, fraudCase.status);
        assertEquals("compliance_officer", fraudCase.resolvedBy);
        assertNotNull(fraudCase.resolvedAt);

        // Cleanup
        fraudCaseService.delete(fraudCase.id);
    }

    @Test
    @DisplayName("Should handle complete customer case workflow from creation to closure")
    @Transactional
    void shouldHandleCompleteCustomerCaseWorkflow() {
        // Given
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        CustomerCaseRequest request = new CustomerCaseRequest(
            testUserId, testAccountNumber, CustomerCase.CaseType.TRANSACTION_DISPUTE,
            CustomerCase.Priority.URGENT,
            "Unauthorized transaction of IDR 10.000.000",
            "I never made this transaction",
            "Customer called hotline, very distressed"
        );

        CustomerCase customerCase = customerCaseService.create(request);
        assertEquals(CustomerCase.CaseStatus.OPEN, customerCase.status);
        assertEquals(CustomerCase.Priority.URGENT, customerCase.priority);

        // When - Assign to agent
        customerCase = customerCaseService.assign(customerCase.id, "senior_agent");
        assertEquals(CustomerCase.CaseStatus.IN_PROGRESS, customerCase.status);

        // When - Update with findings
        customerCase = customerCaseService.update(
            customerCase.id,
            new id.payu.backoffice.dto.CustomerCaseUpdateRequest(
                CustomerCase.CaseStatus.IN_PROGRESS,
                "Investigating with payment processor. Evidence gathered."
            ),
            "senior_agent"
        );

        // When - Resolve case
        customerCase = customerCaseService.update(
            customerCase.id,
            new id.payu.backoffice.dto.CustomerCaseUpdateRequest(
                CustomerCase.CaseStatus.RESOLVED,
                "Confirmed unauthorized. Refund processed. Case closed."
            ),
            "senior_agent"
        );

        // Then - Verify final state
        assertEquals(CustomerCase.CaseStatus.RESOLVED, customerCase.status);
        assertEquals("senior_agent", customerCase.resolvedBy);
        assertNotNull(customerCase.resolvedAt);

        // Cleanup
        customerCaseService.delete(customerCase.id);
    }

    // ===== Dashboard Data Tests =====

    @Test
    @DisplayName("Should retrieve paginated KYC reviews for dashboard")
    @Transactional
    void shouldRetrievePaginatedKycReviewsForDashboard() {
        // Given
        String testUserId = "test-user-dash-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-dash-" + System.currentTimeMillis();

        for (int i = 0; i < 3; i++) {
            kycReviewService.create(new KycReviewRequest(
                testUserId + "-dash-" + i,
                testAccountNumber + "-dash-" + i,
                "KTP", "123" + i, null, "User " + i, null, null, "Dashboard test " + i
            ));
        }

        // When
        List<KycReview> page1 = kycReviewService.listAll(0, 10);

        // Then
        assertNotNull(page1);
        assertTrue(page1.stream().anyMatch(r -> r.userId.startsWith(testUserId)));

        // Cleanup
        page1.stream()
            .filter(r -> r.userId.startsWith(testUserId))
            .forEach(r -> kycReviewService.delete(r.id));
    }

    @Test
    @DisplayName("Should retrieve paginated fraud cases for dashboard")
    @Transactional
    void shouldRetrievePaginatedFraudCasesForDashboard() {
        // Given
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
                FraudCase.RiskLevel.MEDIUM,
                "Dashboard fraud test " + i,
                null
            );
        }

        // When
        List<FraudCase> page1 = fraudCaseService.listAll(0, 10);

        // Then
        assertNotNull(page1);
        assertTrue(page1.stream().anyMatch(c -> c.userId.startsWith(testUserId)));

        // Cleanup
        page1.stream()
            .filter(c -> c.userId.startsWith(testUserId))
            .forEach(c -> fraudCaseService.delete(c.id));
    }

    @Test
    @DisplayName("Should retrieve paginated customer cases for dashboard")
    @Transactional
    void shouldRetrievePaginatedCustomerCasesForDashboard() {
        // Given
        String testUserId = "test-user-dash-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-dash-" + System.currentTimeMillis();

        for (int i = 0; i < 3; i++) {
            customerCaseService.create(new CustomerCaseRequest(
                testUserId + "-dash-" + i,
                testAccountNumber + "-dash-" + i,
                CustomerCase.CaseType.GENERAL_INQUIRY,
                CustomerCase.Priority.MEDIUM,
                "Dashboard subject " + i,
                "Dashboard description " + i,
                null
            ));
        }

        // When
        List<CustomerCase> page1 = customerCaseService.listAll(0, 10);

        // Then
        assertNotNull(page1);
        assertTrue(page1.stream().anyMatch(c -> c.userId.startsWith(testUserId)));

        // Cleanup
        page1.stream()
            .filter(c -> c.userId.startsWith(testUserId))
            .forEach(c -> customerCaseService.delete(c.id));
    }
}
