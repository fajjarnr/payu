package id.payu.compliance.adapter.web;

import id.payu.compliance.domain.model.DataAccessAudit;
import id.payu.compliance.domain.port.in.DataAccessAuditUseCase;
import id.payu.compliance.dto.DataAccessAuditRequest;
import id.payu.compliance.dto.DataAccessAuditResponse;
import id.payu.compliance.dto.DataAccessAuditSearchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import id.payu.compliance.domain.model.DataOperationType;

@RestController
@RequestMapping("/api/v1/gdpr-audit")
@RequiredArgsConstructor
@Tag(name = "GDPR Data Access Audit", description = "API for auditing user data access patterns for GDPR compliance")
@SecurityRequirement(name = "bearerAuth")
public class GdprAuditController extends BaseController {

    private final DataAccessAuditUseCase dataAccessAuditUseCase;

    @PostMapping
    @Operation(summary = "Log data access", description = "Record when user data is accessed for GDPR compliance tracking")
    @ApiResponse(responseCode = "201", description = "Data access logged successfully",
            content = @Content(schema = @Schema(implementation = DataAccessAuditResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<id.payu.api.common.response.ApiResponse<DataAccessAuditResponse>> logDataAccess(@Valid @RequestBody DataAccessAuditRequest request) {
        DataAccessAudit audit = dataAccessAuditUseCase.logDataAccess(
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

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{auditId}")
                .buildAndExpand(audit.getId())
                .toUri();

        return created(toResponse(audit), location.toString());
    }

    @GetMapping("/{auditId}")
    @Operation(summary = "Get data access audit by ID", description = "Retrieve a specific data access audit record")
    @ApiResponse(responseCode = "200", description = "Data access audit found",
            content = @Content(schema = @Schema(implementation = DataAccessAuditResponse.class)))
    @ApiResponse(responseCode = "404", description = "Data access audit not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<id.payu.api.common.response.ApiResponse<DataAccessAuditResponse>> getDataAccessAudit(
            @Parameter(description = "Audit ID", required = true) @PathVariable UUID auditId) {
        DataAccessAudit audit = dataAccessAuditUseCase.getDataAccessAudit(auditId);
        return ok(toResponse(audit));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user data access history", description = "Retrieve all data access records for a specific user")
    @ApiResponse(responseCode = "200", description = "Data access history retrieved successfully",
            content = @Content(schema = @Schema(implementation = DataAccessAuditResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER') or #userId == authentication.principal.userId")
    public ResponseEntity<id.payu.api.common.response.ApiResponse<Page<DataAccessAuditResponse>>> getUserDataAccessHistory(
            @Parameter(description = "User ID", required = true) @PathVariable String userId,
            @Parameter(description = "Page number (default: 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20)") @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("accessedAt").descending());
        Page<DataAccessAudit> audits = dataAccessAuditUseCase.getUserDataAccessHistory(userId, pageable);
        return ok(audits.map(this::toResponse), audits);
    }

    @GetMapping("/users/{userId}/date-range")
    @Operation(summary = "Get user data access by date range", description = "Retrieve data access records for a user within a date range")
    @ApiResponse(responseCode = "200", description = "Data access records retrieved successfully",
            content = @Content(schema = @Schema(implementation = DataAccessAuditResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER') or #userId == authentication.principal.userId")
    public ResponseEntity<id.payu.api.common.response.ApiResponse<List<DataAccessAuditResponse>>> getUserDataAccessByDateRange(
            @Parameter(description = "User ID", required = true) @PathVariable String userId,
            @Parameter(description = "Start date") @RequestParam LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam LocalDateTime endDate
    ) {
        List<DataAccessAudit> audits = dataAccessAuditUseCase.getUserDataAccessHistoryByDateRange(userId, startDate, endDate);
        return ok(audits.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/accessed-by/{accessedBy}")
    @Operation(summary = "Get access by user history", description = "Retrieve records showing what data a specific user has accessed")
    @ApiResponse(responseCode = "200", description = "Access history retrieved successfully",
            content = @Content(schema = @Schema(implementation = DataAccessAuditResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<id.payu.api.common.response.ApiResponse<List<DataAccessAuditResponse>>> getAccessedByUserHistory(
            @Parameter(description = "Accessed by user ID", required = true) @PathVariable String accessedBy,
            @Parameter(description = "Start date") @RequestParam LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam LocalDateTime endDate
    ) {
        List<DataAccessAudit> audits = dataAccessAuditUseCase.getAccessedByUserHistory(accessedBy, startDate, endDate);
        return ok(audits.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/operations/{operationType}")
    @Operation(summary = "Get data access by operation type", description = "Retrieve all data access records of a specific operation type")
    @ApiResponse(responseCode = "200", description = "Data access records retrieved successfully",
            content = @Content(schema = @Schema(implementation = DataAccessAuditResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<id.payu.api.common.response.ApiResponse<Page<DataAccessAuditResponse>>> getDataAccessByOperationType(
            @Parameter(description = "Operation type", required = true) @PathVariable String operationType,
            @Parameter(description = "Page number (default: 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20)") @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("accessedAt").descending());
        Page<DataAccessAudit> audits = dataAccessAuditUseCase.getDataAccessByOperationType(
                DataOperationType.valueOf(operationType.toUpperCase()),
                pageable
        );
        return ok(audits.map(this::toResponse), audits);
    }

    @GetMapping("/services/{serviceName}")
    @Operation(summary = "Get service data access history", description = "Retrieve data access records for a specific service")
    @ApiResponse(responseCode = "200", description = "Service data access history retrieved successfully",
            content = @Content(schema = @Schema(implementation = DataAccessAuditResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<id.payu.api.common.response.ApiResponse<List<DataAccessAuditResponse>>> getServiceDataAccessHistory(
            @Parameter(description = "Service name", required = true) @PathVariable String serviceName,
            @Parameter(description = "Start date") @RequestParam LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam LocalDateTime endDate
    ) {
        List<DataAccessAudit> audits = dataAccessAuditUseCase.getServiceDataAccessHistory(serviceName, startDate, endDate);
        return ok(audits.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/users/{userId}/count")
    @Operation(summary = "Count user data access", description = "Count total data access records for a user since a specific date")
    @ApiResponse(responseCode = "200", description = "Count retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER') or #userId == authentication.principal.userId")
    public ResponseEntity<id.payu.api.common.response.ApiResponse<Long>> getUserDataAccessCount(
            @Parameter(description = "User ID", required = true) @PathVariable String userId,
            @Parameter(description = "Since date") @RequestParam LocalDateTime since
    ) {
        long count = dataAccessAuditUseCase.getUserDataAccessCount(userId, since);
        return ok(count);
    }

    @GetMapping("/failed-access")
    @Operation(summary = "Get failed access attempts", description = "Retrieve all failed data access attempts since a specific date")
    @ApiResponse(responseCode = "200", description = "Failed access attempts retrieved successfully",
            content = @Content(schema = @Schema(implementation = DataAccessAuditResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<id.payu.api.common.response.ApiResponse<List<DataAccessAuditResponse>>> getFailedAccessAttempts(
            @Parameter(description = "Since date") @RequestParam LocalDateTime since
    ) {
        List<DataAccessAudit> audits = dataAccessAuditUseCase.getFailedAccessAttempts(since);
        return ok(audits.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @PostMapping("/search")
    @Operation(summary = "Search data access audits", description = "Search data access audit records with multiple filters")
    @ApiResponse(responseCode = "200", description = "Search results retrieved successfully",
            content = @Content(schema = @Schema(implementation = DataAccessAuditResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<id.payu.api.common.response.ApiResponse<Page<DataAccessAuditResponse>>> searchDataAccessAudit(@RequestBody DataAccessAuditSearchRequest request) {
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

        return ok(audits.map(this::toResponse), audits);
    }

    // BUG-BE-081: DELETE endpoint removed — audit logs are immutable per compliance policy.
    // Use soft-delete with approval workflow if data retention policy requires it.

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
                .success(audit.isSuccess())
                .errorMessage(audit.getErrorMessage())
                .accessedAt(audit.getAccessedAt())
                .createdAt(audit.getCreatedAt())
                .build();
    }
}
