package id.payu.compliance.adapter.web;

import id.payu.compliance.domain.model.DataAccessAudit;
import id.payu.compliance.domain.port.in.DataAccessAuditUseCase;
import id.payu.compliance.dto.DataAccessAuditRequest;
import id.payu.compliance.dto.DataAccessAuditResponse;
import id.payu.compliance.dto.DataAccessAuditSearchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/gdpr-audit")
@RequiredArgsConstructor
@Tag(name = "GDPR Data Access Audit", description = "API for auditing user data access patterns for GDPR compliance")
public class GdprAuditController {

    private final DataAccessAuditUseCase dataAccessAuditUseCase;

    @PostMapping
    @Operation(summary = "Log data access", description = "Record when user data is accessed for GDPR compliance tracking")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<DataAccessAuditResponse> logDataAccess(@Valid @RequestBody DataAccessAuditRequest request) {
        dataAccessAuditUseCase.logDataAccess(
                request.getUserId(),
                request.getAccessedBy(),
                request.getServiceName(),
                request.getResourceType(),
                request.getResourceId(),
                request.getOperationType(),
                request.getPurpose(),
                request.getIpAddress(),
                request.getUserAgent(),
                request.getSuccess() != null ? request.getSuccess() : true,
                request.getErrorMessage()
        );

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{auditId}")
    @Operation(summary = "Get data access audit by ID", description = "Retrieve a specific data access audit record")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<DataAccessAuditResponse> getDataAccessAudit(@PathVariable UUID auditId) {
        DataAccessAudit audit = dataAccessAuditUseCase.getDataAccessAudit(auditId);
        return ResponseEntity.ok(toResponse(audit));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user data access history", description = "Retrieve all data access records for a specific user")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER') or #userId == authentication.principal.userId")
    public ResponseEntity<Page<DataAccessAuditResponse>> getUserDataAccessHistory(
            @PathVariable String userId,
            @Parameter(description = "Page number (default: 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20)") @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("accessedAt").descending());
        Page<DataAccessAudit> audits = dataAccessAuditUseCase.getUserDataAccessHistory(userId, pageable);
        return ResponseEntity.ok(audits.map(this::toResponse));
    }

    @GetMapping("/users/{userId}/date-range")
    @Operation(summary = "Get user data access by date range", description = "Retrieve data access records for a user within a date range")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER') or #userId == authentication.principal.userId")
    public ResponseEntity<List<DataAccessAuditResponse>> getUserDataAccessByDateRange(
            @PathVariable String userId,
            @Parameter(description = "Start date") @RequestParam LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam LocalDateTime endDate
    ) {
        List<DataAccessAudit> audits = dataAccessAuditUseCase.getUserDataAccessHistoryByDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(audits.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/accessed-by/{accessedBy}")
    @Operation(summary = "Get access by user history", description = "Retrieve records showing what data a specific user has accessed")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<List<DataAccessAuditResponse>> getAccessedByUserHistory(
            @PathVariable String accessedBy,
            @Parameter(description = "Start date") @RequestParam LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam LocalDateTime endDate
    ) {
        List<DataAccessAudit> audits = dataAccessAuditUseCase.getAccessedByUserHistory(accessedBy, startDate, endDate);
        return ResponseEntity.ok(audits.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/operations/{operationType}")
    @Operation(summary = "Get data access by operation type", description = "Retrieve all data access records of a specific operation type")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<Page<DataAccessAuditResponse>> getDataAccessByOperationType(
            @PathVariable String operationType,
            @Parameter(description = "Page number (default: 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20)") @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("accessedAt").descending());
        Page<DataAccessAudit> audits = dataAccessAuditUseCase.getDataAccessByOperationType(
                DataAccessAudit.DataOperationType.valueOf(operationType.toUpperCase()),
                pageable
        );
        return ResponseEntity.ok(audits.map(this::toResponse));
    }

    @GetMapping("/services/{serviceName}")
    @Operation(summary = "Get service data access history", description = "Retrieve data access records for a specific service")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<List<DataAccessAuditResponse>> getServiceDataAccessHistory(
            @PathVariable String serviceName,
            @Parameter(description = "Start date") @RequestParam LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam LocalDateTime endDate
    ) {
        List<DataAccessAudit> audits = dataAccessAuditUseCase.getServiceDataAccessHistory(serviceName, startDate, endDate);
        return ResponseEntity.ok(audits.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/users/{userId}/count")
    @Operation(summary = "Count user data access", description = "Count total data access records for a user since a specific date")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER') or #userId == authentication.principal.userId")
    public ResponseEntity<Long> getUserDataAccessCount(
            @PathVariable String userId,
            @Parameter(description = "Since date") @RequestParam LocalDateTime since
    ) {
        long count = dataAccessAuditUseCase.getUserDataAccessCount(userId, since);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/failed-access")
    @Operation(summary = "Get failed access attempts", description = "Retrieve all failed data access attempts since a specific date")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<List<DataAccessAuditResponse>> getFailedAccessAttempts(
            @Parameter(description = "Since date") @RequestParam LocalDateTime since
    ) {
        List<DataAccessAudit> audits = dataAccessAuditUseCase.getFailedAccessAttempts(since);
        return ResponseEntity.ok(audits.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @PostMapping("/search")
    @Operation(summary = "Search data access audits", description = "Search data access audit records with multiple filters")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<Page<DataAccessAuditResponse>> searchDataAccessAudit(@RequestBody DataAccessAuditSearchRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage() != null ? request.getPage() : 0,
                request.getSize() != null ? request.getSize() : 20,
                Sort.by("accessedAt").descending()
        );

        Page<DataAccessAudit> audits = dataAccessAuditUseCase.searchDataAccessAudit(
                request.getUserId(),
                request.getAccessedBy(),
                request.getServiceName(),
                request.getOperationType(),
                request.getStartDate(),
                request.getEndDate(),
                pageable
        );

        return ResponseEntity.ok(audits.map(this::toResponse));
    }

    @DeleteMapping("/{auditId}")
    @Operation(summary = "Delete data access audit", description = "Delete a specific data access audit record")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDataAccessAudit(@PathVariable UUID auditId) {
        dataAccessAuditUseCase.deleteDataAccessAudit(auditId);
        return ResponseEntity.noContent().build();
    }

    private DataAccessAuditResponse toResponse(DataAccessAudit audit) {
        return DataAccessAuditResponse.builder()
                .id(audit.getId())
                .userId(audit.getUserId())
                .accessedBy(audit.getAccessedBy())
                .serviceName(audit.getServiceName())
                .resourceType(audit.getResourceType())
                .resourceId(audit.getResourceId())
                .operationType(audit.getOperationType())
                .purpose(audit.getPurpose())
                .ipAddress(audit.getIpAddress())
                .userAgent(audit.getUserAgent())
                .success(audit.getSuccess())
                .errorMessage(audit.getErrorMessage())
                .accessedAt(audit.getAccessedAt())
                .createdAt(audit.getCreatedAt())
                .build();
    }
}