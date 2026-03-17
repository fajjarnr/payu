package id.payu.backoffice.application.service;

import id.payu.backoffice.domain.FraudCase;
import id.payu.backoffice.dto.FraudCaseDecisionRequest;
import org.junit.jupiter.api.BeforeEach;
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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FraudCaseServiceTest {

    @Autowired
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
        assertNotNull(fraudCase.getId());
        assertEquals(testUserId, fraudCase.getUserId());
        assertEquals("ACC-001", fraudCase.getAccountNumber());
        assertEquals(testTransactionId, fraudCase.getTransactionId());
        assertEquals("TRANSFER", fraudCase.getTransactionType());
        assertEquals(new BigDecimal("1000.00"), fraudCase.getAmount());
        assertEquals("PHISHING", fraudCase.getFraudType());
        assertEquals(FraudCase.RiskLevel.HIGH, fraudCase.getRiskLevel());
        assertEquals(FraudCase.CaseStatus.OPEN, fraudCase.getStatus());
        assertEquals("Suspicious activity detected", fraudCase.getDescription());
        assertEquals("{\"file\": \"logs.txt\"}", fraudCase.getEvidence());
        assertNotNull(fraudCase.getCreatedAt());
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
        assertEquals(FraudCase.RiskLevel.MEDIUM, fraudCase.getRiskLevel());
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

        Optional<FraudCase> result = fraudCaseService.getById(fraudCase.getId());

        assertTrue(result.isPresent());
        assertEquals(fraudCase.getId(), result.get().getId());
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
        assertTrue(results.stream().anyMatch(fc -> fc.getUserId().equals(testUserId)));
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
        assertTrue(results.stream().allMatch(fc -> fc.getStatus() == FraudCase.CaseStatus.OPEN));
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
        assertTrue(results.stream().allMatch(fc -> fc.getRiskLevel() == FraudCase.RiskLevel.CRITICAL));
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

        FraudCase result = fraudCaseService.assign(fraudCase.getId(), "admin1");

        assertNotNull(result);
        assertEquals("admin1", result.getAssignedTo());
        assertEquals(FraudCase.CaseStatus.UNDER_INVESTIGATION, result.getStatus());
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

        FraudCase result = fraudCaseService.resolve(fraudCase.getId(), request, "admin2");

        assertNotNull(result);
        assertEquals(FraudCase.CaseStatus.RESOLVED, result.getStatus());
        assertEquals("Case resolved - confirmed fraud", result.getNotes());
        assertEquals("admin2", result.getResolvedBy());
        assertNotNull(result.getResolvedAt());
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

        FraudCase result = fraudCaseService.resolve(fraudCase.getId(), request, "admin2");

        assertEquals(FraudCase.CaseStatus.CLOSED, result.getStatus());
        assertEquals("False positive", result.getNotes());
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

        FraudCase result = fraudCaseService.resolve(fraudCase.getId(), request, "admin2");

        assertEquals(FraudCase.CaseStatus.ESCALATED, result.getStatus());
        assertEquals("Escalating to legal team", result.getNotes());
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

        FraudCase result = fraudCaseService.resolve(fraudCase.getId(), request, "admin2");

        assertEquals(FraudCase.CaseStatus.UNDER_INVESTIGATION, result.getStatus());
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

        UUID fraudCaseId = fraudCase.getId();
        fraudCaseService.delete(fraudCaseId);

        Optional<FraudCase> result = fraudCaseService.getById(fraudCaseId);
        assertFalse(result.isPresent());
    }
}
