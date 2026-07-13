package id.payu.compliance.unit;

import id.payu.compliance.application.service.ComplianceAuditService;
import id.payu.compliance.domain.model.AuditReport;
import id.payu.compliance.domain.model.ComplianceCheck;
import id.payu.compliance.domain.model.ComplianceCheckResult;
import id.payu.compliance.domain.model.ComplianceStandard;
import id.payu.compliance.domain.port.in.AuditReportUseCase;
import id.payu.compliance.domain.port.out.AuditReportPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplianceAuditServiceTest {

    @Mock
    private AuditReportPersistencePort persistencePort;

    private AuditReportUseCase auditReportUseCase;

    @BeforeEach
    void setUp() {
        auditReportUseCase = new ComplianceAuditService(persistencePort);
    }

    @Test
    void shouldCreatePciDssAuditReportSuccessfully() {
        UUID transactionId = UUID.randomUUID();
        String merchantId = "MERCHANT_001";

        ComplianceCheck cardDataCheck = ComplianceCheck.builder()
                .checkId("PCIDSS_001")
                .standard(ComplianceStandard.PCI_DSS)
                .description("Card data encryption verification")
                .status(ComplianceCheckResult.PASS)
                .details("Card data properly encrypted at rest and in transit")
                .checkedAt(LocalDateTime.now())
                .build();

        ComplianceCheck accessControlCheck = ComplianceCheck.builder()
                .checkId("PCIDSS_002")
                .standard(ComplianceStandard.PCI_DSS)
                .description("Access control verification")
                .status(ComplianceCheckResult.PASS)
                .details("Multi-factor authentication enabled")
                .checkedAt(LocalDateTime.now())
                .build();

        List<ComplianceCheck> checks = List.of(cardDataCheck, accessControlCheck);
        boolean passed = checks.stream().allMatch(c -> c.getStatus() == ComplianceCheckResult.PASS);

        AuditReport expectedReport = AuditReport.builder()
                .transactionId(transactionId)
                .merchantId(merchantId)
                .standard(ComplianceStandard.PCI_DSS)
                .checks(checks)
                .overallStatus(passed ? ComplianceCheckResult.PASS : ComplianceCheckResult.FAIL)
                .createdAt(LocalDateTime.now())
                .build();

        when(persistencePort.save(any(AuditReport.class))).thenReturn(expectedReport);

        AuditReport result = auditReportUseCase.createAuditReport(transactionId, merchantId, ComplianceStandard.PCI_DSS, checks);

        assertNotNull(result);
        assertEquals(transactionId, result.getTransactionId());
        assertEquals(merchantId, result.getMerchantId());
        assertEquals(ComplianceStandard.PCI_DSS, result.getStandard());
        assertEquals(ComplianceCheckResult.PASS, result.getOverallStatus());
        assertEquals(2, result.getChecks().size());

        verify(persistencePort, times(1)).save(any(AuditReport.class));
    }

    @Test
    void shouldCreateOjkAuditReportSuccessfully() {
        UUID transactionId = UUID.randomUUID();
        String merchantId = "MERCHANT_001";

        ComplianceCheck knowYourCustomerCheck = ComplianceCheck.builder()
                .checkId("OJK_001")
                .standard(ComplianceStandard.OJK)
                .description("KYC verification check")
                .status(ComplianceCheckResult.PASS)
                .details("Customer identity verified")
                .checkedAt(LocalDateTime.now())
                .build();

        ComplianceCheck suspiciousActivityCheck = ComplianceCheck.builder()
                .checkId("OJK_002")
                .standard(ComplianceStandard.OJK)
                .description("Suspicious activity monitoring")
                .status(ComplianceCheckResult.PASS)
                .details("No suspicious activity detected")
                .checkedAt(LocalDateTime.now())
                .build();

        List<ComplianceCheck> checks = List.of(knowYourCustomerCheck, suspiciousActivityCheck);
        boolean passed = checks.stream().allMatch(c -> c.getStatus() == ComplianceCheckResult.PASS);

        AuditReport expectedReport = AuditReport.builder()
                .transactionId(transactionId)
                .merchantId(merchantId)
                .standard(ComplianceStandard.OJK)
                .checks(checks)
                .overallStatus(passed ? ComplianceCheckResult.PASS : ComplianceCheckResult.FAIL)
                .createdAt(LocalDateTime.now())
                .build();

        when(persistencePort.save(any(AuditReport.class))).thenReturn(expectedReport);

        AuditReport result = auditReportUseCase.createAuditReport(transactionId, merchantId, ComplianceStandard.OJK, checks);

        assertNotNull(result);
        assertEquals(ComplianceStandard.OJK, result.getStandard());
        assertEquals(2, result.getChecks().size());
        assertEquals(ComplianceCheckResult.PASS, result.getOverallStatus());

        verify(persistencePort, times(1)).save(any(AuditReport.class));
    }

    @Test
    void shouldReturnFailedStatusWhenAnyCheckFails() {
        UUID transactionId = UUID.randomUUID();
        String merchantId = "MERCHANT_001";

        ComplianceCheck passedCheck = ComplianceCheck.builder()
                .checkId("PCIDSS_001")
                .standard(ComplianceStandard.PCI_DSS)
                .description("Card data encryption verification")
                .status(ComplianceCheckResult.PASS)
                .details("Card data properly encrypted")
                .checkedAt(LocalDateTime.now())
                .build();

        ComplianceCheck failedCheck = ComplianceCheck.builder()
                .checkId("PCIDSS_002")
                .standard(ComplianceStandard.PCI_DSS)
                .description("Access control verification")
                .status(ComplianceCheckResult.FAIL)
                .details("Multi-factor authentication not enabled")
                .checkedAt(LocalDateTime.now())
                .build();

        List<ComplianceCheck> checks = List.of(passedCheck, failedCheck);

        AuditReport expectedReport = AuditReport.builder()
                .transactionId(transactionId)
                .merchantId(merchantId)
                .standard(ComplianceStandard.PCI_DSS)
                .checks(checks)
                .overallStatus(ComplianceCheckResult.FAIL)
                .createdAt(LocalDateTime.now())
                .build();

        when(persistencePort.save(any(AuditReport.class))).thenReturn(expectedReport);

        AuditReport result = auditReportUseCase.createAuditReport(transactionId, merchantId, ComplianceStandard.PCI_DSS, checks);

        assertNotNull(result);
        assertEquals(ComplianceCheckResult.FAIL, result.getOverallStatus());
        assertTrue(result.getChecks().stream().anyMatch(c -> c.getStatus() == ComplianceCheckResult.FAIL));
    }

    @Test
    void shouldRetrieveAuditReportById() {
        UUID reportId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        String merchantId = "MERCHANT_001";

        AuditReport expectedReport = AuditReport.builder()
                .id(reportId)
                .transactionId(transactionId)
                .merchantId(merchantId)
                .standard(ComplianceStandard.PCI_DSS)
                .checks(List.of())
                .overallStatus(ComplianceCheckResult.PASS)
                .createdAt(LocalDateTime.now())
                .build();

        when(persistencePort.findById(reportId)).thenReturn(java.util.Optional.of(expectedReport));

        AuditReport result = auditReportUseCase.getAuditReport(reportId);

        assertNotNull(result);
        assertEquals(reportId, result.getId());
        assertEquals(transactionId, result.getTransactionId());
        assertEquals(merchantId, result.getMerchantId());

        verify(persistencePort, times(1)).findById(reportId);
    }

    @Test
    void shouldReturnEmptyWhenReportNotFound() {
        UUID reportId = UUID.randomUUID();

        when(persistencePort.findById(reportId)).thenReturn(java.util.Optional.empty());

        java.util.Optional<AuditReport> result = auditReportUseCase.findAuditReport(reportId);

        assertTrue(result.isEmpty());
        verify(persistencePort, times(1)).findById(reportId);
    }

    @Test
    void shouldRetrieveReportsByTransactionId() {
        UUID transactionId = UUID.randomUUID();
        List<AuditReport> expectedReports = List.of(
                AuditReport.builder()
                        .id(UUID.randomUUID())
                        .transactionId(transactionId)
                        .merchantId("MERCHANT_001")
                        .standard(ComplianceStandard.PCI_DSS)
                        .checks(List.of())
                        .overallStatus(ComplianceCheckResult.PASS)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        when(persistencePort.findByTransactionId(transactionId)).thenReturn(expectedReports);

        List<AuditReport> result = auditReportUseCase.getReportsByTransaction(transactionId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(transactionId, result.get(0).getTransactionId());

        verify(persistencePort, times(1)).findByTransactionId(transactionId);
    }

    @Test
    void shouldRetrieveReportsByMerchantId() {
        String merchantId = "MERCHANT_001";
        List<AuditReport> expectedReports = List.of(
                AuditReport.builder()
                        .id(UUID.randomUUID())
                        .transactionId(UUID.randomUUID())
                        .merchantId(merchantId)
                        .standard(ComplianceStandard.OJK)
                        .checks(List.of())
                        .overallStatus(ComplianceCheckResult.PASS)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        when(persistencePort.findByMerchantId(merchantId)).thenReturn(expectedReports);

        List<AuditReport> result = auditReportUseCase.getReportsByMerchant(merchantId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(merchantId, result.get(0).getMerchantId());

        verify(persistencePort, times(1)).findByMerchantId(merchantId);
    }

    @Test
    void shouldReturnWarningStatusWhenChecksHaveWarnings() {
        UUID transactionId = UUID.randomUUID();
        String merchantId = "MERCHANT_001";

        ComplianceCheck passedCheck = ComplianceCheck.builder()
                .checkId("PCIDSS_001")
                .standard(ComplianceStandard.PCI_DSS)
                .description("Card data encryption verification")
                .status(ComplianceCheckResult.PASS)
                .details("Card data properly encrypted")
                .checkedAt(LocalDateTime.now())
                .build();

        ComplianceCheck warningCheck = ComplianceCheck.builder()
                .checkId("PCIDSS_002")
                .standard(ComplianceStandard.PCI_DSS)
                .description("Access control verification")
                .status(ComplianceCheckResult.WARNING)
                .details("Multi-factor authentication enabled but password policy needs improvement")
                .checkedAt(LocalDateTime.now())
                .build();

        List<ComplianceCheck> checks = List.of(passedCheck, warningCheck);

        AuditReport expectedReport = AuditReport.builder()
                .transactionId(transactionId)
                .merchantId(merchantId)
                .standard(ComplianceStandard.PCI_DSS)
                .checks(checks)
                .overallStatus(ComplianceCheckResult.WARNING)
                .createdAt(LocalDateTime.now())
                .build();

        when(persistencePort.save(any(AuditReport.class))).thenReturn(expectedReport);

        AuditReport result = auditReportUseCase.createAuditReport(transactionId, merchantId, ComplianceStandard.PCI_DSS, checks);

        assertNotNull(result);
        assertEquals(ComplianceCheckResult.WARNING, result.getOverallStatus());
        assertTrue(result.getChecks().stream().anyMatch(c -> c.getStatus() == ComplianceCheckResult.WARNING));
    }
}
