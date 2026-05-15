package id.payu.compliance.application.service;

import id.payu.compliance.domain.model.AuditReport;
import id.payu.compliance.domain.model.ComplianceCheck;
import id.payu.compliance.domain.model.ComplianceCheckResult;
import id.payu.compliance.domain.model.ComplianceStandard;
import id.payu.compliance.domain.port.in.AuditReportUseCase;
import id.payu.compliance.domain.port.out.AuditReportPersistencePort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceAuditService implements AuditReportUseCase {

    private final AuditReportPersistencePort persistencePort;

    @Override
    @CircuitBreaker(name = "compliance", fallbackMethod = "createAuditReportFallback")
    @Retry(name = "compliance")
    @Transactional
    public AuditReport createAuditReport(UUID transactionId, String merchantId, ComplianceStandard standard, List<ComplianceCheck> checks) {
        log.info("Creating {} audit report for transaction: {}, merchant: {}", standard, transactionId, merchantId);

        boolean allPassed = checks.stream().allMatch(c -> c.getStatus() == ComplianceCheckResult.PASS);
        boolean hasFailure = checks.stream().anyMatch(c -> c.getStatus() == ComplianceCheckResult.FAIL);
        boolean hasWarning = checks.stream().anyMatch(c -> c.getStatus() == ComplianceCheckResult.WARNING);

        ComplianceCheckResult overallStatus;
        if (hasFailure) {
            overallStatus = ComplianceCheckResult.FAIL;
        } else if (hasWarning) {
            overallStatus = ComplianceCheckResult.WARNING;
        } else {
            overallStatus = ComplianceCheckResult.PASS;
        }

        AuditReport report = AuditReport.builder()
                .transactionId(transactionId)
                .merchantId(merchantId)
                .standard(standard)
                .checks(checks)
                .overallStatus(overallStatus)
                .createdAt(LocalDateTime.now())
                .build();

        AuditReport savedReport = persistencePort.save(report);

        log.info("Audit report created with ID: {}, overall status: {}", savedReport.getId(), overallStatus);

        return savedReport;
    }

    @Override
    @CircuitBreaker(name = "compliance", fallbackMethod = "getAuditReportFallback")
    @Retry(name = "compliance")
    public AuditReport getAuditReport(UUID reportId) {
        log.info("Retrieving audit report: {}", reportId);
        return persistencePort.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Audit report not found: " + reportId));
    }

    @Override
    @CircuitBreaker(name = "compliance", fallbackMethod = "findAuditReportFallback")
    @Retry(name = "compliance")
    public Optional<AuditReport> findAuditReport(UUID reportId) {
        return persistencePort.findById(reportId);
    }

    @Override
    @CircuitBreaker(name = "compliance", fallbackMethod = "getReportsByTransactionFallback")
    @Retry(name = "compliance")
    public List<AuditReport> getReportsByTransaction(UUID transactionId) {
        log.info("Retrieving audit reports for transaction: {}", transactionId);
        return persistencePort.findByTransactionId(transactionId);
    }

    @Override
    @CircuitBreaker(name = "compliance", fallbackMethod = "getReportsByMerchantFallback")
    @Retry(name = "compliance")
    public List<AuditReport> getReportsByMerchant(String merchantId) {
        log.info("Retrieving audit reports for merchant: {}", merchantId);
        return persistencePort.findByMerchantId(merchantId);
    }

    private AuditReport createAuditReportFallback(UUID transactionId, String merchantId, ComplianceStandard standard, List<ComplianceCheck> checks, Exception ex) {
        log.error("Fallback for createAuditReport: {}", ex.getMessage());
        throw new RuntimeException("Compliance service temporarily unavailable", ex);
    }

    private AuditReport getAuditReportFallback(UUID reportId, Exception ex) {
        log.error("Fallback for getAuditReport: {}", ex.getMessage());
        throw new RuntimeException("Compliance service temporarily unavailable", ex);
    }

    private Optional<AuditReport> findAuditReportFallback(UUID reportId, Exception ex) {
        log.error("Fallback for findAuditReport: {}", ex.getMessage());
        throw new RuntimeException("Compliance service temporarily unavailable", ex);
    }

    private List<AuditReport> getReportsByTransactionFallback(UUID transactionId, Exception ex) {
        log.error("Fallback for getReportsByTransaction: {}", ex.getMessage());
        throw new RuntimeException("Compliance service temporarily unavailable", ex);
    }

    private List<AuditReport> getReportsByMerchantFallback(String merchantId, Exception ex) {
        log.error("Fallback for getReportsByMerchant: {}", ex.getMessage());
        throw new RuntimeException("Compliance service temporarily unavailable", ex);
    }
}
