package id.payu.backoffice.integration;

import id.payu.backoffice.adapter.persistence.entity.CustomerCaseEntity;
import id.payu.backoffice.adapter.persistence.entity.FraudCaseEntity;
import id.payu.backoffice.adapter.persistence.entity.KycReviewEntity;
import id.payu.backoffice.dto.CustomerCaseRequest;
import id.payu.backoffice.dto.FraudCaseDecisionRequest;
import id.payu.backoffice.dto.KycReviewDecisionRequest;
import id.payu.backoffice.dto.KycReviewRequest;
import id.payu.backoffice.application.service.CustomerCaseService;
import id.payu.backoffice.application.service.FraudCaseService;
import id.payu.backoffice.application.service.KycReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import id.payu.backoffice.testutil.IntegrationTest;

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
@SpringBootTest
@ActiveProfiles("integrationtest")
@IntegrationTest
@DisplayName("Backoffice Service Integration Tests")
@Tag("integration")
class BackofficeIntegrationTest {

    @Autowired
    KycReviewService kycReviewService;

    @Autowired
    FraudCaseService fraudCaseService;

    @Autowired
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
        KycReviewEntity createdReview = kycReviewService.create(request);
        Optional<KycReviewEntity> retrievedReview = kycReviewService.getById(createdReview.getId());

        // Then
        assertTrue(retrievedReview.isPresent());
        assertEquals(createdReview.getId(), retrievedReview.get().getId());
        assertEquals(testUserId, retrievedReview.get().getUserId());
        assertEquals(testAccountNumber, retrievedReview.get().getAccountNumber());
        assertEquals("PASSPORT", retrievedReview.get().getDocumentType());
        assertEquals(KycReviewEntity.KycStatus.PENDING, retrievedReview.get().getStatus());
        assertNotNull(retrievedReview.get().getCreatedAt());

        // Cleanup
        kycReviewService.delete(createdReview.getId());
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

        KycReviewEntity review = kycReviewService.create(request);
        KycReviewDecisionRequest decisionRequest = new KycReviewDecisionRequest(
            KycReviewDecisionRequest.KycReviewStatus.APPROVED,
            "Documents verified, identity confirmed"
        );

        // When
        KycReviewEntity result = kycReviewService.review(review.getId(), decisionRequest, "admin1");

        // Then
        assertEquals(KycReviewEntity.KycStatus.APPROVED, result.getStatus());
        assertEquals("Documents verified, identity confirmed", result.getNotes());
        assertEquals("admin1", result.getReviewedBy());
        assertNotNull(result.getReviewedAt());
        assertNotNull(result.getCreatedAt());

        // Cleanup
        kycReviewService.delete(review.getId());
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

        KycReviewEntity review = kycReviewService.create(request);
        KycReviewDecisionRequest decisionRequest = new KycReviewDecisionRequest(
            KycReviewDecisionRequest.KycReviewStatus.REJECTED,
            "Document expired, please submit valid ID"
        );

        // When
        KycReviewEntity result = kycReviewService.review(review.getId(), decisionRequest, "admin2");

        // Then
        assertEquals(KycReviewEntity.KycStatus.REJECTED, result.getStatus());
        assertTrue(result.getNotes().contains("expired"));
        assertEquals("admin2", result.getReviewedBy());

        // Cleanup
        kycReviewService.delete(review.getId());
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
        List<KycReviewEntity> pendingReviews = kycReviewService.listByStatus(KycReviewEntity.KycStatus.PENDING, 0, 10);

        // Then
        assertNotNull(pendingReviews);
        assertTrue(pendingReviews.stream().anyMatch(r -> r.getUserId().startsWith(testUserId)));
        assertTrue(pendingReviews.stream().allMatch(r -> r.getStatus() == KycReviewEntity.KycStatus.PENDING));

        // Cleanup
        pendingReviews.stream()
            .filter(r -> r.getUserId().startsWith(testUserId))
            .forEach(r -> kycReviewService.delete(r.getId()));
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
        FraudCaseEntity createdCase = fraudCaseService.create(
            testUserId,
            testAccountNumber,
            transactionId,
            "TRANSFER",
            amount,
            "ACCOUNT_TAKEOVER",
            FraudCaseEntity.RiskLevel.HIGH,
            "Unusual login pattern followed by large transfer",
            "{\"ip\": \"192.168.1.100\", \"device\": \"unknown\"}"
        );

        Optional<FraudCaseEntity> retrievedCase = fraudCaseService.getById(createdCase.getId());

        // Then
        assertTrue(retrievedCase.isPresent());
        assertEquals(createdCase.getId(), retrievedCase.get().getId());
        assertEquals(testUserId, retrievedCase.get().getUserId());
        assertEquals(transactionId, retrievedCase.get().getTransactionId());
        assertEquals("TRANSFER", retrievedCase.get().getTransactionType());
        assertEquals(amount, retrievedCase.get().getAmount());
        assertEquals(FraudCaseEntity.RiskLevel.HIGH, retrievedCase.get().getRiskLevel());
        assertEquals(FraudCaseEntity.CaseStatus.OPEN, retrievedCase.get().getStatus());

        // Cleanup
        fraudCaseService.delete(createdCase.getId());
    }

    @Test
    @DisplayName("Should assign fraud case to investigator")
    @Transactional
    void shouldAssignFraudCase() {
        // Given
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        FraudCaseEntity fraudCase = fraudCaseService.create(
            testUserId,
            testAccountNumber,
            UUID.randomUUID(),
            "PAYMENT",
            new BigDecimal("5000000"),
            "CARD_FRAUD",
            FraudCaseEntity.RiskLevel.MEDIUM,
            "Multiple failed card attempts",
            null
        );

        // When
        FraudCaseEntity assignedCase = fraudCaseService.assign(fraudCase.getId(), "investigator1");

        // Then
        assertEquals("investigator1", assignedCase.getAssignedTo());
        assertEquals(FraudCaseEntity.CaseStatus.UNDER_INVESTIGATION, assignedCase.getStatus());

        // Cleanup
        fraudCaseService.delete(fraudCase.getId());
    }

    @Test
    @DisplayName("Should resolve fraud case as confirmed fraud")
    @Transactional
    void shouldResolveFraudCaseAsConfirmed() {
        // Given
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        FraudCaseEntity fraudCase = fraudCaseService.create(
            testUserId,
            testAccountNumber,
            UUID.randomUUID(),
            "TRANSFER",
            new BigDecimal("10000000"),
            "PHISHING",
            FraudCaseEntity.RiskLevel.CRITICAL,
            "Customer reports phishing attack",
            "{\"email\": \"scam@fake.com\"}"
        );

        fraudCaseService.assign(fraudCase.getId(), "investigator2");

        FraudCaseDecisionRequest decisionRequest = new FraudCaseDecisionRequest(
            FraudCaseDecisionRequest.FraudCaseStatus.RESOLVED,
            "Confirmed phishing attack. Account credentials compromised. Action taken."
        );

        // When
        FraudCaseEntity resolvedCase = fraudCaseService.resolve(fraudCase.getId(), decisionRequest, "investigator2");

        // Then
        assertEquals(FraudCaseEntity.CaseStatus.RESOLVED, resolvedCase.getStatus());
        assertEquals("investigator2", resolvedCase.getResolvedBy());
        assertNotNull(resolvedCase.getResolvedAt());

        // Cleanup
        fraudCaseService.delete(fraudCase.getId());
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
            new BigDecimal("5000000"), "FRAUD_A", FraudCaseEntity.RiskLevel.HIGH,
            "High risk case", null
        );

        fraudCaseService.create(
            testUserId + "-2", testAccountNumber + "-2", UUID.randomUUID(), "PAYMENT",
            new BigDecimal("3000000"), "FRAUD_B", FraudCaseEntity.RiskLevel.HIGH,
            "Another high risk case", null
        );

        // When
        List<FraudCaseEntity> highRiskCases = fraudCaseService.listByRiskLevel(FraudCaseEntity.RiskLevel.HIGH, 0, 10);

        // Then
        assertNotNull(highRiskCases);
        assertTrue(highRiskCases.stream().anyMatch(c -> c.getUserId().startsWith(testUserId)));
        assertTrue(highRiskCases.stream().filter(c -> c.getUserId().startsWith(testUserId))
            .allMatch(c -> c.getRiskLevel() == FraudCaseEntity.RiskLevel.HIGH));

        // Cleanup
        highRiskCases.stream()
            .filter(c -> c.getUserId().startsWith(testUserId))
            .forEach(c -> fraudCaseService.delete(c.getId()));
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
            CustomerCaseEntity.CaseType.TRANSACTION_DISPUTE,
            CustomerCaseEntity.Priority.HIGH,
            "Unauthorized transaction on my account",
            "I see a transaction I didn't make",
            "Customer called support"
        );

        // When
        CustomerCaseEntity createdCase = customerCaseService.create(request);
        Optional<CustomerCaseEntity> retrievedCase = customerCaseService.getById(createdCase.getId());

        // Then
        assertTrue(retrievedCase.isPresent());
        assertEquals(createdCase.getId(), retrievedCase.get().getId());
        assertEquals(testUserId, retrievedCase.get().getUserId());
        assertEquals(CustomerCaseEntity.CaseType.TRANSACTION_DISPUTE, retrievedCase.get().getCaseType());
        assertEquals(CustomerCaseEntity.Priority.HIGH, retrievedCase.get().getPriority());
        assertEquals(CustomerCaseEntity.CaseStatus.OPEN, retrievedCase.get().getStatus());
        assertNotNull(retrievedCase.get().getCaseNumber());

        // Cleanup
        customerCaseService.delete(createdCase.getId());
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
            CustomerCaseEntity.CaseType.ACCOUNT_ISSUE,
            CustomerCaseEntity.Priority.MEDIUM,
            "Cannot access account",
            "Login fails with correct credentials",
            null
        );

        CustomerCaseEntity customerCase = customerCaseService.create(request);

        // When
        CustomerCaseEntity assignedCase = customerCaseService.assign(customerCase.getId(), "agent1");

        // Then
        assertEquals("agent1", assignedCase.getAssignedTo());
        assertEquals(CustomerCaseEntity.CaseStatus.IN_PROGRESS, assignedCase.getStatus());

        // Cleanup
        customerCaseService.delete(customerCase.getId());
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
            CustomerCaseEntity.CaseType.TECHNICAL_ISSUE,
            CustomerCaseEntity.Priority.LOW,
            "App not loading",
            "Mobile app crashes on startup",
            null
        );

        CustomerCaseEntity customerCase = customerCaseService.create(request);
        customerCaseService.assign(customerCase.getId(), "support1");

        // When
        CustomerCaseEntity updatedCase = customerCaseService.update(
            customerCase.getId(),
            new id.payu.backoffice.dto.CustomerCaseUpdateRequest(
                CustomerCaseEntity.CaseStatus.RESOLVED,
                "Fixed in version 2.1. Please update app"
            ),
            "support1"
        );

        // Then
        assertEquals(CustomerCaseEntity.CaseStatus.RESOLVED, updatedCase.getStatus());
        assertEquals("support1", updatedCase.getResolvedBy());
        assertNotNull(updatedCase.getResolvedAt());
        assertTrue(updatedCase.getNotes().contains("version 2.1"));

        // Cleanup
        customerCaseService.delete(customerCase.getId());
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
            CustomerCaseEntity.CaseType.TRANSACTION_DISPUTE, CustomerCaseEntity.Priority.URGENT,
            "Urgent case 1", "Description 1", null
        ));

        customerCaseService.create(new CustomerCaseRequest(
            testUserId + "-2", testAccountNumber + "-2",
            CustomerCaseEntity.CaseType.ACCOUNT_ISSUE, CustomerCaseEntity.Priority.URGENT,
            "Urgent case 2", "Description 2", null
        ));

        // When
        List<CustomerCaseEntity> urgentCases = customerCaseService.listByPriority(CustomerCaseEntity.Priority.URGENT, 0, 10);

        // Then
        assertNotNull(urgentCases);
        assertTrue(urgentCases.stream().anyMatch(c -> c.getUserId().startsWith(testUserId)));
        assertTrue(urgentCases.stream().filter(c -> c.getUserId().startsWith(testUserId))
            .allMatch(c -> c.getPriority() == CustomerCaseEntity.Priority.URGENT));

        // Cleanup
        urgentCases.stream()
            .filter(c -> c.getUserId().startsWith(testUserId))
            .forEach(c -> customerCaseService.delete(c.getId()));
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

        KycReviewEntity review = kycReviewService.create(request);

        // When
        KycReviewDecisionRequest decisionRequest = new KycReviewDecisionRequest(
            KycReviewDecisionRequest.KycReviewStatus.APPROVED,
            "Approved after verification"
        );
        KycReviewEntity updatedReview = kycReviewService.review(review.getId(), decisionRequest, "admin_audit");

        // Then - Verify audit trail is maintained
        assertNotNull(updatedReview.getCreatedAt());
        assertNotNull(updatedReview.getReviewedAt());
        assertEquals("admin_audit", updatedReview.getReviewedBy());

        // Cleanup
        kycReviewService.delete(review.getId());
    }

    @Test
    @DisplayName("Should maintain audit trail for fraud case operations")
    @Transactional
    void shouldMaintainAuditTrailForFraudOperations() {
        // Given
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        FraudCaseEntity fraudCase = fraudCaseService.create(
            testUserId, testAccountNumber, UUID.randomUUID(), "TRANSFER",
            new BigDecimal("5000000"), "FRAUD", FraudCaseEntity.RiskLevel.HIGH,
            "Audit test", null
        );

        // When
        fraudCaseService.assign(fraudCase.getId(), "investigator_audit");

        FraudCaseDecisionRequest decisionRequest = new FraudCaseDecisionRequest(
            FraudCaseDecisionRequest.FraudCaseStatus.CLOSED,
            "Case closed after investigation"
        );
        FraudCaseEntity resolvedCase = fraudCaseService.resolve(fraudCase.getId(), decisionRequest, "investigator_audit");

        // Then - Verify audit trail
        assertNotNull(resolvedCase.getCreatedAt());
        assertEquals("investigator_audit", resolvedCase.getAssignedTo());
        assertEquals("investigator_audit", resolvedCase.getResolvedBy());
        assertNotNull(resolvedCase.getResolvedAt());

        // Cleanup
        fraudCaseService.delete(fraudCase.getId());
    }

    @Test
    @DisplayName("Should maintain audit trail for customer case operations")
    @Transactional
    void shouldMaintainAuditTrailForCustomerOperations() {
        // Given
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        CustomerCaseRequest request = new CustomerCaseRequest(
            testUserId, testAccountNumber, CustomerCaseEntity.CaseType.ACCOUNT_ISSUE,
            CustomerCaseEntity.Priority.HIGH, "Audit test", "Audit description", null
        );

        CustomerCaseEntity customerCase = customerCaseService.create(request);

        // When
        customerCaseService.assign(customerCase.getId(), "agent_audit");
        CustomerCaseEntity updatedCase = customerCaseService.update(
            customerCase.getId(),
            new id.payu.backoffice.dto.CustomerCaseUpdateRequest(
                CustomerCaseEntity.CaseStatus.RESOLVED,
                "Issue resolved"
            ),
            "agent_audit"
        );

        // Then - Verify audit trail
        assertNotNull(updatedCase.getCreatedAt());
        assertEquals("agent_audit", updatedCase.getAssignedTo());
        assertEquals("agent_audit", updatedCase.getResolvedBy());
        assertNotNull(updatedCase.getResolvedAt());

        // Cleanup
        customerCaseService.delete(customerCase.getId());
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
                CustomerCaseEntity.CaseStatus.IN_PROGRESS,
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
        KycReviewEntity review = kycReviewService.create(request);
        assertEquals(KycReviewEntity.KycStatus.PENDING, review.getStatus());

        // When - Request additional info
        KycReviewDecisionRequest infoRequest = new KycReviewDecisionRequest(
            KycReviewDecisionRequest.KycReviewStatus.REQUIRES_ADDITIONAL_INFO,
            "Please provide proof of address"
        );
        review = kycReviewService.review(review.getId(), infoRequest, "admin1");
        assertEquals(KycReviewEntity.KycStatus.REQUIRES_ADDITIONAL_INFO, review.getStatus());

        // When - Final approval
        KycReviewDecisionRequest approvalRequest = new KycReviewDecisionRequest(
            KycReviewDecisionRequest.KycReviewStatus.APPROVED,
            "All documents verified and approved"
        );
        review = kycReviewService.review(review.getId(), approvalRequest, "admin2");

        // Then - Verify final state
        assertEquals(KycReviewEntity.KycStatus.APPROVED, review.getStatus());
        assertEquals("admin2", review.getReviewedBy());
        assertNotNull(review.getReviewedAt());

        // Cleanup
        kycReviewService.delete(review.getId());
    }

    @Test
    @DisplayName("Should handle complete fraud case workflow from detection to resolution")
    @Transactional
    void shouldHandleCompleteFraudWorkflow() {
        // Given
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        FraudCaseEntity fraudCase = fraudCaseService.create(
            testUserId, testAccountNumber, UUID.randomUUID(), "TRANSFER",
            new BigDecimal("25000000"), "MONEY_LAUNDERING",
            FraudCaseEntity.RiskLevel.CRITICAL,
            "Suspicious large transaction pattern detected",
            "{\"pattern\": \"layering\", \"alerts\": 5}"
        );

        assertEquals(FraudCaseEntity.CaseStatus.OPEN, fraudCase.getStatus());

        // When - Assign to investigator
        fraudCase = fraudCaseService.assign(fraudCase.getId(), "senior_investigator");
        assertEquals(FraudCaseEntity.CaseStatus.UNDER_INVESTIGATION, fraudCase.getStatus());

        // When - Escalate for further review
        FraudCaseDecisionRequest escalateRequest = new FraudCaseDecisionRequest(
            FraudCaseDecisionRequest.FraudCaseStatus.ESCALATED,
            "Complex case requiring compliance team review"
        );
        fraudCase = fraudCaseService.resolve(fraudCase.getId(), escalateRequest, "senior_investigator");
        assertEquals(FraudCaseEntity.CaseStatus.ESCALATED, fraudCase.getStatus());

        // When - Final resolution
        FraudCaseDecisionRequest resolveRequest = new FraudCaseDecisionRequest(
            FraudCaseDecisionRequest.FraudCaseStatus.CLOSED,
            "Case reviewed and closed. SAR filed."
        );
        fraudCase = fraudCaseService.resolve(fraudCase.getId(), resolveRequest, "compliance_officer");

        // Then - Verify final state
        assertEquals(FraudCaseEntity.CaseStatus.CLOSED, fraudCase.getStatus());
        assertEquals("compliance_officer", fraudCase.getResolvedBy());
        assertNotNull(fraudCase.getResolvedAt());

        // Cleanup
        fraudCaseService.delete(fraudCase.getId());
    }

    @Test
    @DisplayName("Should handle complete customer case workflow from creation to closure")
    @Transactional
    void shouldHandleCompleteCustomerCaseWorkflow() {
        // Given
        String testUserId = "test-user-" + System.currentTimeMillis();
        String testAccountNumber = "ACC-" + System.currentTimeMillis();

        CustomerCaseRequest request = new CustomerCaseRequest(
            testUserId, testAccountNumber, CustomerCaseEntity.CaseType.TRANSACTION_DISPUTE,
            CustomerCaseEntity.Priority.URGENT,
            "Unauthorized transaction of IDR 10.000.000",
            "I never made this transaction",
            "Customer called hotline, very distressed"
        );

        CustomerCaseEntity customerCase = customerCaseService.create(request);
        assertEquals(CustomerCaseEntity.CaseStatus.OPEN, customerCase.getStatus());
        assertEquals(CustomerCaseEntity.Priority.URGENT, customerCase.getPriority());

        // When - Assign to agent
        customerCase = customerCaseService.assign(customerCase.getId(), "senior_agent");
        assertEquals(CustomerCaseEntity.CaseStatus.IN_PROGRESS, customerCase.getStatus());

        // When - Update with findings
        customerCase = customerCaseService.update(
            customerCase.getId(),
            new id.payu.backoffice.dto.CustomerCaseUpdateRequest(
                CustomerCaseEntity.CaseStatus.IN_PROGRESS,
                "Investigating with payment processor. Evidence gathered."
            ),
            "senior_agent"
        );

        // When - Resolve case
        customerCase = customerCaseService.update(
            customerCase.getId(),
            new id.payu.backoffice.dto.CustomerCaseUpdateRequest(
                CustomerCaseEntity.CaseStatus.RESOLVED,
                "Confirmed unauthorized. Refund processed. Case closed."
            ),
            "senior_agent"
        );

        // Then - Verify final state
        assertEquals(CustomerCaseEntity.CaseStatus.RESOLVED, customerCase.getStatus());
        assertEquals("senior_agent", customerCase.getResolvedBy());
        assertNotNull(customerCase.getResolvedAt());

        // Cleanup
        customerCaseService.delete(customerCase.getId());
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
        List<KycReviewEntity> page1 = kycReviewService.listAll(0, 10);

        // Then
        assertNotNull(page1);
        assertTrue(page1.stream().anyMatch(r -> r.getUserId().startsWith(testUserId)));

        // Cleanup
        page1.stream()
            .filter(r -> r.getUserId().startsWith(testUserId))
            .forEach(r -> kycReviewService.delete(r.getId()));
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
                FraudCaseEntity.RiskLevel.MEDIUM,
                "Dashboard fraud test " + i,
                null
            );
        }

        // When
        List<FraudCaseEntity> page1 = fraudCaseService.listAll(0, 10);

        // Then
        assertNotNull(page1);
        assertTrue(page1.stream().anyMatch(c -> c.getUserId().startsWith(testUserId)));

        // Cleanup
        page1.stream()
            .filter(c -> c.getUserId().startsWith(testUserId))
            .forEach(c -> fraudCaseService.delete(c.getId()));
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
                CustomerCaseEntity.CaseType.GENERAL_INQUIRY,
                CustomerCaseEntity.Priority.MEDIUM,
                "Dashboard subject " + i,
                "Dashboard description " + i,
                null
            ));
        }

        // When
        List<CustomerCaseEntity> page1 = customerCaseService.listAll(0, 10);

        // Then
        assertNotNull(page1);
        assertTrue(page1.stream().anyMatch(c -> c.getUserId().startsWith(testUserId)));

        // Cleanup
        page1.stream()
            .filter(c -> c.getUserId().startsWith(testUserId))
            .forEach(c -> customerCaseService.delete(c.getId()));
    }
}
