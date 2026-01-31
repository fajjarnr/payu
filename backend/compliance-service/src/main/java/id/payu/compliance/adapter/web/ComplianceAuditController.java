package id.payu.compliance.adapter.web;

import id.payu.compliance.application.service.ComplianceAuditService;
import id.payu.compliance.domain.model.AuditReport;
import id.payu.compliance.domain.model.ComplianceCheck;
import id.payu.compliance.domain.model.ComplianceStandard;
import id.payu.compliance.dto.AuditReportRequest;
import id.payu.compliance.dto.AuditReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/compliance")
@Slf4j
@Tag(name = "Compliance Audit", description = "Compliance audit and reporting APIs for regulatory standards (PCI-DSS, ISO27001, etc.)")
@SecurityRequirement(name = "bearerAuth")
public class ComplianceAuditController extends BaseController {

    private ComplianceAuditService complianceAuditService;

    public ComplianceAuditController(ComplianceAuditService complianceAuditService) {
        this.complianceAuditService = complianceAuditService;
    }

    public void setComplianceAuditService(ComplianceAuditService complianceAuditService) {
        this.complianceAuditService = complianceAuditService;
    }

    @PostMapping("/audit-report")
    @Operation(summary = "Create audit report", description = "Create a compliance audit report for a transaction")
    @ApiResponse(responseCode = "201", description = "Audit report created successfully",
            content = @Content(schema = @Schema(implementation = AuditReportResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden - requires COMPLIANCE_OFFICER or ADMIN role")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER') or hasRole('ADMIN')")
    public ResponseEntity<id.payu.api.common.response.ApiResponse<AuditReportResponse>> createAuditReport(@Valid @RequestBody AuditReportRequest request) {
        log.info("Creating {} audit report for transaction: {}", request.standard(), request.transactionId());

        AuditReport report = complianceAuditService.createAuditReport(
                request.transactionId(),
                request.merchantId(),
                request.standard(),
                request.checks()
        );

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{reportId}")
                .buildAndExpand(report.getId())
                .toUri();

        return created(toResponse(report), location.toString());
    }

    @GetMapping("/audit-report/{id}")
    @Operation(summary = "Get audit report by ID", description = "Retrieve a compliance audit report by its ID")
    @ApiResponse(responseCode = "200", description = "Audit report found",
            content = @Content(schema = @Schema(implementation = AuditReportResponse.class)))
    @ApiResponse(responseCode = "404", description = "Audit report not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden - requires COMPLIANCE_OFFICER or ADMIN role")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER') or hasRole('ADMIN')")
    public ResponseEntity<id.payu.api.common.response.ApiResponse<AuditReportResponse>> getAuditReport(
            @Parameter(description = "Audit report ID", required = true) @PathVariable UUID id) {
        log.info("Retrieving audit report: {}", id);

        AuditReport report = complianceAuditService.getAuditReport(id);

        return ok(toResponse(report));
    }

    @GetMapping("/audit-report")
    @Operation(summary = "Search audit reports", description = "Search compliance audit reports by transaction ID, merchant ID, or standard with optional date filtering")
    @ApiResponse(responseCode = "200", description = "Audit reports retrieved successfully",
            content = @Content(schema = @Schema(implementation = AuditReportResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request - at least one search parameter required")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden - requires COMPLIANCE_OFFICER or ADMIN role")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER') or hasRole('ADMIN')")
    public ResponseEntity<id.payu.api.common.response.ApiResponse<List<AuditReportResponse>>> searchAuditReports(
            @Parameter(description = "Transaction ID to filter by") @RequestParam(required = false) UUID transactionId,
            @Parameter(description = "Merchant ID to filter by") @RequestParam(required = false) String merchantId,
            @Parameter(description = "Compliance standard to filter by (PCI_DSS, ISO27001, GDPR, etc.)") @RequestParam(required = false) ComplianceStandard standard,
            @Parameter(description = "Start date for filtering (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @Parameter(description = "End date for filtering (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

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

        return ok(response);
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
