package id.payu.compliance.integration;

import id.payu.compliance.application.service.ComplianceAuditService;
import id.payu.compliance.application.service.DataAccessAuditService;
import id.payu.compliance.domain.model.AuditReport;
import id.payu.compliance.domain.model.ComplianceCheck;
import id.payu.compliance.domain.model.ComplianceCheckResult;
import id.payu.compliance.domain.model.ComplianceStandard;
import id.payu.compliance.domain.model.DataAccessAudit;
import id.payu.compliance.domain.model.DataAccessAudit.DataOperationType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Compliance Service using Testcontainers with PostgreSQL.
 *
 * These tests verify the complete AML (Anti-Money Laundering), CFT (Counter-Terrorism Financing),
 * and audit trail functionality against a real PostgreSQL database, ensuring proper:
 *
 * 1. Transaction screening against sanctions lists
 * 2. Suspicious transaction detection
 * 3. Audit trail creation and retrieval
 * 4. Compliance report generation
 * 5. Various AML check scenarios (pass, warning, fail)
 *
 * Uses @Tag("integration") to allow selective test execution.
 *
 * @author PayU Backend Team
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@DisplayName("Compliance Service Integration Tests")
@Tag("integration")
class ComplianceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ComplianceAuditService complianceAuditService;

    @Autowired
    private DataAccessAuditService dataAccessAuditService;

    @BeforeAll
    static void startContainer() {
        postgres.start();
    }

    @AfterAll
    static void stopContainer() {
        postgres.stop();
    }

    @Test
    @DisplayName("Should create and retrieve AML audit report successfully")
    void shouldCreateAndRetrieveAMLReport() {
        // Given
        UUID transactionId = UUID.randomUUID();
        String merchantId = "MERCHANT_001";

        List<ComplianceCheck> checks = List.of(
            createComplianceCheck("AML_001", "Sanctions List Screening", ComplianceCheckResult.PASS),
            createComplianceCheck("AML_002", "Transaction Pattern Analysis", ComplianceCheckResult.PASS),
            createComplianceCheck("AML_003", "PEP Screening", ComplianceCheckResult.PASS),
            createComplianceCheck("CFT_001", "Terrorist Financing Check", ComplianceCheckResult.PASS)
        );

        // When
        AuditReport report = complianceAuditService.createAuditReport(
            transactionId,
            merchantId,
            ComplianceStandard.AML,
            checks
        );

        // Then
        assertThat(report).isNotNull();
        assertThat(report.getId()).isNotNull();
        assertThat(report.getTransactionId()).isEqualTo(transactionId);
        assertThat(report.getMerchantId()).isEqualTo(merchantId);
        assertThat(report.getStandard()).isEqualTo(ComplianceStandard.AML);
        assertThat(report.getOverallStatus()).isEqualTo(ComplianceCheckResult.PASS);
        assertThat(report.getChecks()).hasSize(4);
        assertThat(report.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should retrieve audit report by ID from database")
    void shouldRetrieveAuditReportById() {
        // Given
        UUID transactionId = UUID.randomUUID();
        String merchantId = "MERCHANT_002";

        List<ComplianceCheck> checks = List.of(
            createComplianceCheck("AML_001", "Sanctions List Screening", ComplianceCheckResult.PASS)
        );

        AuditReport createdReport = complianceAuditService.createAuditReport(
            transactionId,
            merchantId,
            ComplianceStandard.AML,
            checks
        );

        // When
        AuditReport retrievedReport = complianceAuditService.getAuditReport(createdReport.getId());

        // Then
        assertThat(retrievedReport).isNotNull();
        assertThat(retrievedReport.getId()).isEqualTo(createdReport.getId());
        assertThat(retrievedReport.getTransactionId()).isEqualTo(transactionId);
        assertThat(retrievedReport.getMerchantId()).isEqualTo(merchantId);
        assertThat(retrievedReport.getStandard()).isEqualTo(ComplianceStandard.AML);
        assertThat(retrievedReport.getChecks()).hasSize(1);
        assertThat(retrievedReport.getChecks().get(0).getCheckId()).isEqualTo("AML_001");
    }

    @Test
    @DisplayName("Should retrieve all audit reports by transaction ID")
    void shouldRetrieveReportsByTransactionId() {
        // Given
        UUID transactionId = UUID.randomUUID();
        String merchantId = "MERCHANT_003";

        List<ComplianceCheck> amlChecks = List.of(
            createComplianceCheck("AML_001", "Sanctions Screening", ComplianceCheckResult.PASS)
        );

        List<ComplianceCheck> pciChecks = List.of(
            createComplianceCheck("PCI_001", "Data Encryption Check", ComplianceCheckResult.PASS)
        );

        complianceAuditService.createAuditReport(transactionId, merchantId, ComplianceStandard.AML, amlChecks);
        complianceAuditService.createAuditReport(transactionId, merchantId, ComplianceStandard.PCI_DSS, pciChecks);

        // When
        List<AuditReport> reports = complianceAuditService.getReportsByTransaction(transactionId);

        // Then
        assertThat(reports).hasSize(2);
        assertThat(reports)
            .allSatisfy(report -> assertThat(report.getTransactionId()).isEqualTo(transactionId));
        assertThat(reports)
            .anySatisfy(report -> assertThat(report.getStandard()).isEqualTo(ComplianceStandard.AML));
        assertThat(reports)
            .anySatisfy(report -> assertThat(report.getStandard()).isEqualTo(ComplianceStandard.PCI_DSS));
    }

    @Test
    @DisplayName("Should retrieve all audit reports by merchant ID")
    void shouldRetrieveReportsByMerchantId() {
        // Given
        String merchantId = "MERCHANT_004";

        List<ComplianceCheck> checks = List.of(
            createComplianceCheck("AML_001", "Sanctions Screening", ComplianceCheckResult.PASS)
        );

        complianceAuditService.createAuditReport(UUID.randomUUID(), merchantId, ComplianceStandard.AML, checks);
        complianceAuditService.createAuditReport(UUID.randomUUID(), merchantId, ComplianceStandard.AML, checks);
        complianceAuditService.createAuditReport(UUID.randomUUID(), merchantId, ComplianceStandard.CFT, checks);

        // When
        List<AuditReport> reports = complianceAuditService.getReportsByMerchant(merchantId);

        // Then
        assertThat(reports).hasSize(3);
        assertThat(reports)
            .allSatisfy(report -> assertThat(report.getMerchantId()).isEqualTo(merchantId));
    }

    @Test
    @DisplayName("Transaction should pass AML check when all checks pass")
    void transactionShouldPassAMLCheck() {
        // Given - Normal transaction pattern
        UUID transactionId = UUID.randomUUID();
        String merchantId = "MERCHANT_005";
        String accountId = "ACC_1234567890";

        List<ComplianceCheck> checks = List.of(
            createComplianceCheck("AML_001", "Sanctions List Screening", ComplianceCheckResult.PASS),
            createComplianceCheck("AML_002", "PEP Screening", ComplianceCheckResult.PASS),
            createComplianceCheck("AML_003", "Transaction Amount Check", ComplianceCheckResult.PASS),
            createComplianceCheck("AML_004", "Frequency Analysis", ComplianceCheckResult.PASS),
            createComplianceCheck("AML_005", "Geographic Risk Assessment", ComplianceCheckResult.PASS)
        );

        // When
        AuditReport report = complianceAuditService.createAuditReport(
            transactionId,
            merchantId,
            ComplianceStandard.AML,
            checks
        );

        // Then
        assertThat(report.getOverallStatus()).isEqualTo(ComplianceCheckResult.PASS);
        assertThat(report.getChecks()).hasSize(5);
        assertThat(report.getChecks())
            .allSatisfy(check -> assertThat(check.getStatus()).isEqualTo(ComplianceCheckResult.PASS));
    }

    @Test
    @DisplayName("Transaction should be flagged for manual review when warnings exist")
    void transactionShouldFlagForManualReview() {
        // Given - Suspicious but not clearly fraudulent transaction
        UUID transactionId = UUID.randomUUID();
        String merchantId = "MERCHANT_006";

        List<ComplianceCheck> checks = List.of(
            createComplianceCheck("AML_001", "Sanctions List Screening", ComplianceCheckResult.PASS),
            createComplianceCheck("AML_002", "PEP Screening", ComplianceCheckResult.WARNING),
            createComplianceCheck("AML_003", "Transaction Amount Check", ComplianceCheckResult.PASS),
            createComplianceCheck("AML_004", "Frequency Analysis", ComplianceCheckResult.WARNING),
            createComplianceCheck("AML_005", "Geographic Risk Assessment", ComplianceCheckResult.PASS)
        );

        // When
        AuditReport report = complianceAuditService.createAuditReport(
            transactionId,
            merchantId,
            ComplianceStandard.AML,
            checks
        );

        // Then
        assertThat(report.getOverallStatus()).isEqualTo(ComplianceCheckResult.WARNING);
        assertThat(report.getChecks()).hasSize(5);
        assertThat(report.getChecks().stream())
            .filteredOn(check -> check.getStatus() == ComplianceCheckResult.WARNING)
            .hasSize(2);
    }

    @Test
    @DisplayName("Transaction should be blocked when sanctioned entity detected")
    void transactionShouldBeBlockedForSanctionedEntity() {
        // Given - Matched sanctions list
        UUID transactionId = UUID.randomUUID();
        String merchantId = "MERCHANT_007";

        List<ComplianceCheck> checks = List.of(
            createComplianceCheck("AML_001", "Sanctions List Screening", ComplianceCheckResult.FAIL,
                "Entity matched OFAC SDN List"),
            createComplianceCheck("AML_002", "PEP Screening", ComplianceCheckResult.PASS),
            createComplianceCheck("AML_003", "Transaction Amount Check", ComplianceCheckResult.PASS)
        );

        // When
        AuditReport report = complianceAuditService.createAuditReport(
            transactionId,
            merchantId,
            ComplianceStandard.AML,
            checks
        );

        // Then
        assertThat(report.getOverallStatus()).isEqualTo(ComplianceCheckResult.FAIL);
        assertThat(report.getChecks()).hasSize(3);
        assertThat(report.getChecks().get(0).getStatus()).isEqualTo(ComplianceCheckResult.FAIL);
        assertThat(report.getChecks().get(0).getDetails())
            .contains("OFAC SDN List");
    }

    @Test
    @DisplayName("Should detect suspicious transaction pattern for CFT")
    void shouldDetectSuspiciousTransactionForCFT() {
        // Given - Potential terrorist financing pattern
        UUID transactionId = UUID.randomUUID();
        String merchantId = "MERCHANT_008";

        List<ComplianceCheck> checks = List.of(
            createComplianceCheck("CFT_001", "Terrorist Financing Check", ComplianceCheckResult.WARNING,
                "High-risk jurisdiction transaction"),
            createComplianceCheck("CFT_002", "Structuring Detection", ComplianceCheckResult.WARNING,
                "Multiple just-below-threshold transactions"),
            createComplianceCheck("CFT_003", "Wire Transfer Analysis", ComplianceCheckResult.PASS)
        );

        // When
        AuditReport report = complianceAuditService.createAuditReport(
            transactionId,
            merchantId,
            ComplianceStandard.CFT,
            checks
        );

        // Then
        assertThat(report.getOverallStatus()).isEqualTo(ComplianceCheckResult.WARNING);
        assertThat(report.getChecks()).hasSize(3);
        assertThat(report.getChecks().stream())
            .filteredOn(check -> check.getStatus() == ComplianceCheckResult.WARNING)
            .hasSize(2);
    }

    @Test
    @DisplayName("Should create comprehensive PCI-DSS compliance report")
    void shouldCreatePCIDSSComplianceReport() {
        // Given
        UUID transactionId = UUID.randomUUID();
        String merchantId = "MERCHANT_009";

        List<ComplianceCheck> checks = List.of(
            createComplianceCheck("PCI_001", "Data Encryption at Rest", ComplianceCheckResult.PASS),
            createComplianceCheck("PCI_002", "Data Encryption in Transit", ComplianceCheckResult.PASS),
            createComplianceCheck("PCI_003", "Access Control", ComplianceCheckResult.PASS),
            createComplianceCheck("PCI_004", "Logging and Monitoring", ComplianceCheckResult.PASS),
            createComplianceCheck("PCI_005", "Vulnerability Management", ComplianceCheckResult.PASS),
            createComplianceCheck("PCI_006", "Network Security", ComplianceCheckResult.PASS)
        );

        // When
        AuditReport report = complianceAuditService.createAuditReport(
            transactionId,
            merchantId,
            ComplianceStandard.PCI_DSS,
            checks
        );

        // Then
        assertThat(report.getOverallStatus()).isEqualTo(ComplianceCheckResult.PASS);
        assertThat(report.getStandard()).isEqualTo(ComplianceStandard.PCI_DSS);
        assertThat(report.getChecks()).hasSize(6);
    }

    @Test
    @DisplayName("Should create OJK compliance report")
    void shouldCreateOJKComplianceReport() {
        // Given
        UUID transactionId = UUID.randomUUID();
        String merchantId = "MERCHANT_010";

        List<ComplianceCheck> checks = List.of(
            createComplianceCheck("OJK_001", "Capital Adequacy", ComplianceCheckResult.PASS),
            createComplianceCheck("OJK_002", "Liquidity Ratio", ComplianceCheckResult.PASS),
            createComplianceCheck("OJK_003", "Reporting Requirements", ComplianceCheckResult.PASS)
        );

        // When
        AuditReport report = complianceAuditService.createAuditReport(
            transactionId,
            merchantId,
            ComplianceStandard.OJK,
            checks
        );

        // Then
        assertThat(report.getStandard()).isEqualTo(ComplianceStandard.OJK);
        assertThat(report.getOverallStatus()).isEqualTo(ComplianceCheckResult.PASS);
        assertThat(report.getChecks()).hasSize(3);
    }

    @Test
    @DisplayName("Should create GDPR compliance report")
    void shouldCreateGDPRComplianceReport() {
        // Given
        UUID transactionId = UUID.randomUUID();
        String merchantId = "MERCHANT_011";

        List<ComplianceCheck> checks = List.of(
            createComplianceCheck("GDPR_001", "Consent Management", ComplianceCheckResult.PASS),
            createComplianceCheck("GDPR_002", "Data Subject Rights", ComplianceCheckResult.PASS),
            createComplianceCheck("GDPR_003", "Data Breach Notification", ComplianceCheckResult.PASS),
            createComplianceCheck("GDPR_004", "Data Minimization", ComplianceCheckResult.WARNING)
        );

        // When
        AuditReport report = complianceAuditService.createAuditReport(
            transactionId,
            merchantId,
            ComplianceStandard.GDPR,
            checks
        );

        // Then
        assertThat(report.getStandard()).isEqualTo(ComplianceStandard.GDPR);
        assertThat(report.getOverallStatus()).isEqualTo(ComplianceCheckResult.WARNING);
        assertThat(report.getChecks()).hasSize(4);
    }

    @Test
    @DisplayName("Should create data access audit record")
    void shouldCreateDataAccessAuditRecord() {
        // Given
        String userId = "USER_001";
        String accessedBy = "ADMIN_USER";
        String serviceName = "compliance-service";
        String resourceType = "AuditReport";
        String resourceId = UUID.randomUUID().toString();

        // When
        dataAccessAuditService.logDataAccess(
            userId,
            accessedBy,
            serviceName,
            resourceType,
            resourceId,
            DataOperationType.READ,
            "Compliance audit review",
            "192.168.1.100",
            "Mozilla/5.0",
            true,
            null
        );

        // Then — verify the audit record was persisted by querying user access history
        LocalDateTime since = LocalDateTime.now().minusMinutes(1);
        long accessCount = dataAccessAuditService.getUserDataAccessCount(userId, since);
        assertThat(accessCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should record failed data access attempt")
    void shouldRecordFailedDataAccessAttempt() {
        // Given
        String userId = "USER_002";
        String accessedBy = "UNAUTHORIZED_USER";
        String resourceType = "AuditReport";
        String resourceId = UUID.randomUUID().toString();

        // When - no exception should be thrown
        dataAccessAuditService.logDataAccess(
            userId,
            accessedBy,
            "compliance-service",
            resourceType,
            resourceId,
            DataOperationType.READ,
            "Unauthorized access attempt",
            "192.168.1.200",
            "Mozilla/5.0",
            false,
            "Access denied: insufficient permissions"
        );

        // Then - verify failed access attempts can be retrieved
        LocalDateTime since = LocalDateTime.now().minusMinutes(1);
        List<DataAccessAudit> failedAttempts = dataAccessAuditService.getFailedAccessAttempts(since);

        assertThat(failedAttempts).isNotEmpty();
        assertThat(failedAttempts)
            .anySatisfy(audit -> {
                assertThat(audit.getSuccess()).isFalse();
                assertThat(audit.getErrorMessage()).isEqualTo("Access denied: insufficient permissions");
                assertThat(audit.getOperationType()).isEqualTo(DataOperationType.READ);
            });
    }

    @Test
    @DisplayName("Should record different data operation types")
    void shouldRecordDifferentOperationTypes() {
        // Given
        String userId = "USER_003";
        String resourceId = UUID.randomUUID().toString();

        // When
        dataAccessAuditService.logDataAccess(
            userId, "ADMIN", "service", "Resource", resourceId,
            DataOperationType.READ, "Read operation", "IP1", "Agent", true, null
        );

        dataAccessAuditService.logDataAccess(
            userId, "ADMIN", "service", "Resource", resourceId,
            DataOperationType.UPDATE, "Update operation", "IP1", "Agent", true, null
        );

        dataAccessAuditService.logDataAccess(
            userId, "ADMIN", "service", "Resource", resourceId,
            DataOperationType.DELETE, "Delete operation", "IP1", "Agent", true, null
        );

        dataAccessAuditService.logDataAccess(
            userId, "ADMIN", "service", "Resource", resourceId,
            DataOperationType.EXPORT, "Export operation", "IP1", "Agent", true, null
        );

        dataAccessAuditService.logDataAccess(
            userId, "ADMIN", "service", "Resource", resourceId,
            DataOperationType.SEARCH, "Search operation", "IP1", "Agent", true, null
        );

        // Then - verify user data access history
        LocalDateTime since = LocalDateTime.now().minusMinutes(1);
        long accessCount = dataAccessAuditService.getUserDataAccessCount(userId, since);

        assertThat(accessCount).isGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("Should handle NOT_APPLICABLE compliance checks")
    void shouldHandleNotApplicableChecks() {
        // Given
        UUID transactionId = UUID.randomUUID();
        String merchantId = "MERCHANT_012";

        List<ComplianceCheck> checks = List.of(
            createComplianceCheck("AML_001", "Sanctions Screening", ComplianceCheckResult.PASS),
            createComplianceCheck("AML_002", "International Wire Check", ComplianceCheckResult.NOT_APPLICABLE,
                "Domestic transaction only")
        );

        // When
        AuditReport report = complianceAuditService.createAuditReport(
            transactionId,
            merchantId,
            ComplianceStandard.AML,
            checks
        );

        // Then
        assertThat(report.getOverallStatus()).isEqualTo(ComplianceCheckResult.PASS);
        assertThat(report.getChecks()).hasSize(2);
        assertThat(report.getChecks().get(1).getStatus()).isEqualTo(ComplianceCheckResult.NOT_APPLICABLE);
    }

    @Test
    @DisplayName("Should generate comprehensive compliance report with mixed results")
    void shouldGenerateComprehensiveComplianceReport() {
        // Given - Complex scenario with multiple standards
        UUID transactionId = UUID.randomUUID();
        String merchantId = "MERCHANT_013";

        List<ComplianceCheck> amlChecks = List.of(
            createComplianceCheck("AML_001", "Sanctions Screening", ComplianceCheckResult.PASS),
            createComplianceCheck("AML_002", "Transaction Monitoring", ComplianceCheckResult.WARNING,
                "Unusual transaction pattern detected"),
            createComplianceCheck("AML_003", "Customer Due Diligence", ComplianceCheckResult.PASS)
        );

        // When
        AuditReport amlReport = complianceAuditService.createAuditReport(
            transactionId,
            merchantId,
            ComplianceStandard.AML,
            amlChecks
        );

        // Then
        assertThat(amlReport.getOverallStatus()).isEqualTo(ComplianceCheckResult.WARNING);
        assertThat(amlReport.getChecks()).hasSize(3);
        assertThat(amlReport.getChecks().get(1).getDetails())
            .contains("Unusual transaction pattern");
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    /**
     * Helper method to create ComplianceCheck objects with default values.
     */
    private ComplianceCheck createComplianceCheck(String checkId, String description, ComplianceCheckResult status) {
        return ComplianceCheck.builder()
            .checkId(checkId)
            .standard(ComplianceStandard.AML)
            .description(description)
            .status(status)
            .checkedAt(LocalDateTime.now())
            .build();
    }

    /**
     * Helper method to create ComplianceCheck objects with details.
     */
    private ComplianceCheck createComplianceCheck(String checkId, String description, ComplianceCheckResult status, String details) {
        return ComplianceCheck.builder()
            .checkId(checkId)
            .standard(ComplianceStandard.AML)
            .description(description)
            .status(status)
            .details(details)
            .checkedAt(LocalDateTime.now())
            .build();
    }
}
