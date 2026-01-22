package id.payu.compliance.adapter.web;

import id.payu.compliance.application.service.ComplianceAuditService;
import id.payu.compliance.domain.model.AuditReport;
import id.payu.compliance.domain.model.ComplianceCheck;
import id.payu.compliance.domain.model.ComplianceStandard;
import id.payu.compliance.dto.AuditReportRequest;
import id.payu.compliance.dto.AuditReportResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/compliance")
@Slf4j
public class ComplianceAuditController {

    private ComplianceAuditService complianceAuditService;

    public ComplianceAuditController(ComplianceAuditService complianceAuditService) {
        this.complianceAuditService = complianceAuditService;
    }

    public void setComplianceAuditService(ComplianceAuditService complianceAuditService) {
        this.complianceAuditService = complianceAuditService;
    }

    @PostMapping("/audit-report")
    public ResponseEntity<AuditReportResponse> createAuditReport(@Valid @RequestBody AuditReportRequest request) {
        log.info("Creating {} audit report for transaction: {}", request.standard(), request.transactionId());

        AuditReport report = complianceAuditService.createAuditReport(
                request.transactionId(),
                request.merchantId(),
                request.standard(),
                request.checks()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(report));
    }

    @GetMapping("/audit-report/{id}")
    public ResponseEntity<AuditReportResponse> getAuditReport(@PathVariable UUID id) {
        log.info("Retrieving audit report: {}", id);

        AuditReport report = complianceAuditService.getAuditReport(id);

        return ResponseEntity.ok(toResponse(report));
    }

    @GetMapping("/audit-report")
    public ResponseEntity<List<AuditReportResponse>> searchAuditReports(
            @RequestParam(required = false) UUID transactionId,
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) ComplianceStandard standard,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        log.info("Searching audit reports with filters: transactionId={}, merchantId={}, standard={}", 
                transactionId, merchantId, standard);

        List<AuditReport> reports;

        if (transactionId != null) {
            reports = complianceAuditService.getReportsByTransaction(transactionId);
        } else if (merchantId != null) {
            reports = complianceAuditService.getReportsByMerchant(merchantId);
        } else {
            throw new IllegalArgumentException("At least one search parameter is required");
        }

        List<AuditReportResponse> response = reports.stream()
                .filter(report -> standard == null || report.getStandard() == standard)
                .filter(report -> fromDate == null || !report.getCreatedAt().isBefore(fromDate))
                .filter(report -> toDate == null || !report.getCreatedAt().isAfter(toDate))
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    private AuditReportResponse toResponse(AuditReport report) {
        return new AuditReportResponse(
                report.getId(),
                report.getTransactionId(),
                report.getMerchantId(),
                report.getStandard(),
                report.getChecks(),
                report.getOverallStatus(),
                report.getCreatedAt(),
                report.getCreatedBy()
        );
    }
}
