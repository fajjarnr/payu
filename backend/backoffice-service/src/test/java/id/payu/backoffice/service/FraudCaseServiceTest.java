package id.payu.backoffice.service;

import id.payu.backoffice.domain.FraudCase;
import id.payu.backoffice.dto.FraudCaseDecisionRequest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class FraudCaseServiceTest {

    @Inject
    FraudCaseService fraudCaseService;

    private UUID testTransactionId;
    private String testUserId;

    @BeforeEach
    void setUp() {
        testTransactionId = UUID.randomUUID();
        testUserId = "test-user-" + System.currentTimeMillis();
    }

    // Create Fraud Case Tests

    @Test
    @Transactional
    void testCreateFraudCase_Success() {
        FraudCase fraudCase = fraudCaseService.create(
                testUserId,
                "ACC-001",
                testTransactionId,
                "TRANSFER",
                new BigDecimal("1000.00"),
                "PHISHING",
                FraudCase.RiskLevel.HIGH,
                "Suspicious activity detected",
                "{\"file\": \"logs.txt\"}"
        );

        assertNotNull(fraudCase);
        assertNotNull(fraudCase.id);
        assertEquals(testUserId, fraudCase.userId);
        assertEquals("ACC-001", fraudCase.accountNumber);
        assertEquals(testTransactionId, fraudCase.transactionId);
        assertEquals("TRANSFER", fraudCase.transactionType);
        assertEquals(new BigDecimal("1000.00"), fraudCase.amount);
        assertEquals("PHISHING", fraudCase.fraudType);
        assertEquals(FraudCase.RiskLevel.HIGH, fraudCase.riskLevel);
        assertEquals(FraudCase.CaseStatus.OPEN, fraudCase.status);
        assertEquals("Suspicious activity detected", fraudCase.description);
        assertEquals("{\"file\": \"logs.txt\"}", fraudCase.evidence);
        assertNotNull(fraudCase.createdAt);
    }

    @Test
    @Transactional
    void testCreateFraudCase_WithDefaultRiskLevel() {
        FraudCase fraudCase = fraudCaseService.create(
                testUserId,
                "ACC-002",
                testTransactionId,
                "WITHDRAWAL",
                new BigDecimal("500.00"),
                "ACCOUNT_TAKEOVER",
                null,
                "Potential account takeover",
                "{\"logs\": \"transactions.log\"}"
        );

        assertNotNull(fraudCase);
        assertEquals(FraudCase.RiskLevel.MEDIUM, fraudCase.riskLevel);
    }

    // Query Fraud Case Tests

    @Test
    @Transactional
    void testGetById_Success() {
        FraudCase fraudCase = fraudCaseService.create(
                testUserId,
                "ACC-QUERY",
                testTransactionId,
                "TRANSFER",
                new BigDecimal("2000.00"),
                "MONEY_LAUNDERING",
                FraudCase.RiskLevel.CRITICAL,
                "Suspicious pattern",
                null
        );

        Optional<FraudCase> result = fraudCaseService.getById(fraudCase.id);

        assertTrue(result.isPresent());
        assertEquals(fraudCase.id, result.get().id);
    }

    @Test
    void testGetById_NotFound() {
        Optional<FraudCase> result = fraudCaseService.getById(UUID.randomUUID());

        assertFalse(result.isPresent());
    }

    @Test
    @Transactional
    void testGetByUserId_Success() {
        fraudCaseService.create(
                testUserId,
                "ACC-USER",
                testTransactionId,
                "TRANSFER",
                new BigDecimal("2000.00"),
                "MONEY_LAUNDERING",
                FraudCase.RiskLevel.CRITICAL,
                "Suspicious pattern",
                null
        );

        List<FraudCase> results = fraudCaseService.getByUserId(testUserId);

        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(fc -> fc.userId.equals(testUserId)));
    }

    @Test
    @Transactional
    void testListByStatus_Success() {
        fraudCaseService.create(
                testUserId,
                "ACC-STATUS",
                testTransactionId,
                "TRANSFER",
                new BigDecimal("2000.00"),
                "MONEY_LAUNDERING",
                FraudCase.RiskLevel.CRITICAL,
                "Suspicious pattern",
                null
        );

        List<FraudCase> results = fraudCaseService.listByStatus(FraudCase.CaseStatus.OPEN, 0, 10);

        assertNotNull(results);
        assertTrue(results.stream().allMatch(fc -> fc.status == FraudCase.CaseStatus.OPEN));
    }

    @Test
    @Transactional
    void testListByRiskLevel_Success() {
        fraudCaseService.create(
                testUserId,
                "ACC-RISK",
                testTransactionId,
                "TRANSFER",
                new BigDecimal("2000.00"),
                "MONEY_LAUNDERING",
                FraudCase.RiskLevel.CRITICAL,
                "Suspicious pattern",
                null
        );

        List<FraudCase> results = fraudCaseService.listByRiskLevel(FraudCase.RiskLevel.CRITICAL, 0, 10);

        assertNotNull(results);
        assertTrue(results.stream().allMatch(fc -> fc.riskLevel == FraudCase.RiskLevel.CRITICAL));
    }

    @Test
    @Transactional
    void testListAll_Success() {
        fraudCaseService.create(
                testUserId,
                "ACC-ALL",
                testTransactionId,
                "TRANSFER",
                new BigDecimal("2000.00"),
                "MONEY_LAUNDERING",
                FraudCase.RiskLevel.CRITICAL,
                "Suspicious pattern",
                null
        );

        List<FraudCase> results = fraudCaseService.listAll(0, 10);

        assertNotNull(results);
        assertTrue(results.size() >= 1);
    }

    // Assign Fraud Case Tests

    @Test
    @Transactional
    void testAssign_Success() {
        FraudCase fraudCase = fraudCaseService.create(
                testUserId,
                "ACC-ASSIGN",
                testTransactionId,
                "TRANSFER",
                new BigDecimal("3000.00"),
                "PHISHING",
                FraudCase.RiskLevel.HIGH,
                "Needs investigation",
                null
        );

        FraudCase result = fraudCaseService.assign(fraudCase.id, "admin1");

        assertNotNull(result);
        assertEquals("admin1", result.assignedTo);
        assertEquals(FraudCase.CaseStatus.UNDER_INVESTIGATION, result.status);
    }

    @Test
    @Transactional
    void testAssign_NotFound() {
        assertThrows(IllegalArgumentException.class, () -> {
            fraudCaseService.assign(UUID.randomUUID(), "admin1");
        });
    }

    // Resolve Fraud Case Tests

    @Test
    @Transactional
    void testResolve_AsResolved() {
        FraudCase fraudCase = fraudCaseService.create(
                testUserId,
                "ACC-RESOLVE",
                testTransactionId,
                "TRANSFER",
                new BigDecimal("4000.00"),
                "FRAUD",
                FraudCase.RiskLevel.HIGH,
                "Fraud detected",
                null
        );

        FraudCaseDecisionRequest request = new FraudCaseDecisionRequest(
                FraudCaseDecisionRequest.FraudCaseStatus.RESOLVED,
                "Case resolved - confirmed fraud"
        );

        FraudCase result = fraudCaseService.resolve(fraudCase.id, request, "admin2");

        assertNotNull(result);
        assertEquals(FraudCase.CaseStatus.RESOLVED, result.status);
        assertEquals("Case resolved - confirmed fraud", result.notes);
        assertEquals("admin2", result.resolvedBy);
        assertNotNull(result.resolvedAt);
    }

    @Test
    @Transactional
    void testResolve_AsClosed() {
        FraudCase fraudCase = fraudCaseService.create(
                testUserId,
                "ACC-CLOSE",
                testTransactionId,
                "TRANSFER",
                new BigDecimal("4000.00"),
                "FRAUD",
                FraudCase.RiskLevel.HIGH,
                "Fraud detected",
                null
        );

        FraudCaseDecisionRequest request = new FraudCaseDecisionRequest(
                FraudCaseDecisionRequest.FraudCaseStatus.CLOSED,
                "False positive"
        );

        FraudCase result = fraudCaseService.resolve(fraudCase.id, request, "admin2");

        assertEquals(FraudCase.CaseStatus.CLOSED, result.status);
        assertEquals("False positive", result.notes);
    }

    @Test
    @Transactional
    void testResolve_AsEscalated() {
        FraudCase fraudCase = fraudCaseService.create(
                testUserId,
                "ACC-ESCALATE",
                testTransactionId,
                "TRANSFER",
                new BigDecimal("4000.00"),
                "FRAUD",
                FraudCase.RiskLevel.HIGH,
                "Fraud detected",
                null
        );

        FraudCaseDecisionRequest request = new FraudCaseDecisionRequest(
                FraudCaseDecisionRequest.FraudCaseStatus.ESCALATED,
                "Escalating to legal team"
        );

        FraudCase result = fraudCaseService.resolve(fraudCase.id, request, "admin2");

        assertEquals(FraudCase.CaseStatus.ESCALATED, result.status);
        assertEquals("Escalating to legal team", result.notes);
    }

    @Test
    @Transactional
    void testResolve_AsUnderInvestigation() {
        FraudCase fraudCase = fraudCaseService.create(
                testUserId,
                "ACC-INV",
                testTransactionId,
                "TRANSFER",
                new BigDecimal("4000.00"),
                "FRAUD",
                FraudCase.RiskLevel.HIGH,
                "Fraud detected",
                null
        );

        FraudCaseDecisionRequest request = new FraudCaseDecisionRequest(
                FraudCaseDecisionRequest.FraudCaseStatus.UNDER_INVESTIGATION,
                "Need more evidence"
        );

        FraudCase result = fraudCaseService.resolve(fraudCase.id, request, "admin2");

        assertEquals(FraudCase.CaseStatus.UNDER_INVESTIGATION, result.status);
    }

    @Test
    @Transactional
    void testResolve_NotFound() {
        FraudCaseDecisionRequest request = new FraudCaseDecisionRequest(
                FraudCaseDecisionRequest.FraudCaseStatus.RESOLVED,
                "Test"
        );

        assertThrows(IllegalArgumentException.class, () -> {
            fraudCaseService.resolve(UUID.randomUUID(), request, "admin1");
        });
    }

    // Delete Fraud Case Tests

    @Test
    @Transactional
    void testDelete_Success() {
        FraudCase fraudCase = fraudCaseService.create(
                testUserId,
                "ACC-DELETE",
                testTransactionId,
                "TRANSFER",
                new BigDecimal("5000.00"),
                "TEST_FRAUD",
                FraudCase.RiskLevel.LOW,
                "Test delete",
                null
        );

        UUID fraudCaseId = fraudCase.id;
        fraudCaseService.delete(fraudCaseId);

        Optional<FraudCase> result = fraudCaseService.getById(fraudCaseId);
        assertFalse(result.isPresent());
    }
}
