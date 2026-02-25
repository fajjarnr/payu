package id.payu.backoffice.adapter.web;

import id.payu.backoffice.domain.CustomerCase;
import id.payu.backoffice.domain.FraudCase;
import id.payu.backoffice.domain.KycReview;
import id.payu.backoffice.dto.*;
import id.payu.backoffice.application.service.CustomerCaseService;
import id.payu.backoffice.application.service.FraudCaseService;
import id.payu.backoffice.application.service.KycReviewService;
import id.payu.backoffice.application.service.UniversalSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for backoffice operations including KYC reviews, fraud case management,
 * customer case management, and universal search.
 */
@RestController
@RequestMapping("/api/v1/backoffice")
@Tag(name = "Backoffice Operations", description = "Internal management operations for KYC review, fraud monitoring, and customer support")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class BackofficeController extends BaseController {

    private final KycReviewService kycReviewService;
    private final FraudCaseService fraudCaseService;
    private final CustomerCaseService customerCaseService;
    private final UniversalSearchService universalSearchService;

    /**
     * Resolves admin user identity from X-Admin-User header or authenticated principal.
     * Never falls back to "system" — requires traceable identity for audit trail.
     */
    private String resolveAdminUser(String headerAdminUser) {
        if (headerAdminUser != null && !headerAdminUser.isEmpty()) {
            return headerAdminUser;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            return auth.getName();
        }
        return "unknown-admin";
    }

    // ==================== KYC Review Endpoints ====================

    @PostMapping("/kyc-reviews")
    @PreAuthorize("hasAnyAuthority('admin', 'backoffice')")
    @Operation(
            summary = "Create KYC review",
            description = "Creates a new KYC (Know Your Customer) review for manual verification. " +
                    "This initiates the review process for customer identity verification."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "KYC review created successfully",
                    content = @Content(schema = @Schema(implementation = KycReviewResponse.class)),
                    headers = @Header(
                            name = "Location",
                            description = "URL of the newly created KYC review",
                            schema = @Schema(type = "string")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = id.payu.backoffice.dto.ApiResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(schema = @Schema(implementation = id.payu.backoffice.dto.ApiResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = id.payu.backoffice.dto.ApiResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = id.payu.backoffice.dto.ApiResponse.class))
            )
    })
    public ResponseEntity<id.payu.backoffice.dto.ApiResponse<KycReviewResponse>> createKycReview(
            @Valid @RequestBody
            @Parameter(
                    description = "KYC review request details",
                    required = true,
                    content = @Content(schema = @Schema(implementation = KycReviewRequest.class))
            )
            KycReviewRequest request) {
        LOG.info("Creating KYC review for user: {}", request.userId());
        var review = kycReviewService.create(request);
        return created(KycReviewResponse.from(review), "/api/v1/backoffice/kyc-reviews/" + review.getId());
    }

    @GetMapping("/kyc-reviews/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'backoffice')")
    @Operation(
            summary = "Get KYC review by ID",
            description = "Retrieves details of a specific KYC review including current status and review history."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "KYC review retrieved successfully",
                    content = @Content(schema = @Schema(implementation = KycReviewResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(schema = @Schema(implementation = id.payu.backoffice.dto.ApiResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = id.payu.backoffice.dto.ApiResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "KYC review not found",
                    content = @Content(schema = @Schema(implementation = id.payu.backoffice.dto.ApiResponse.class))
            )
    })
    public ResponseEntity<id.payu.backoffice.dto.ApiResponse<KycReviewResponse>> getKycReview(
            @Parameter(
                    description = "KYC review ID",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathVariable("id") UUID id) {
        return kycReviewService.getById(id)
                .map(review -> ok(KycReviewResponse.from(review)))
                .orElse(notFound("KYC review"));
    }

    @GetMapping("/kyc-reviews")
    @PreAuthorize("hasAnyAuthority('admin', 'backoffice')")
    @Operation(
            summary = "List KYC reviews",
            description = "Retrieves a paginated list of KYC reviews. Can be filtered by status."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of KYC reviews",
                    content = @Content(schema = @Schema(implementation = KycReviewResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = id.payu.backoffice.dto.ApiResponse.class))
            )
    })
    public ResponseEntity<id.payu.backoffice.dto.ApiResponse<List<KycReviewResponse>>> listKycReviews(
            @Parameter(
                    description = "Filter by status (PENDING, UNDER_REVIEW, APPROVED, REJECTED, REQUIRES_ADDITIONAL_INFO)",
                    example = "PENDING"
            )
            @RequestParam(value = "status", required = false) String status,
            @Parameter(
                    description = "Page number (0-based)",
                    example = "0"
            )
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(
                    description = "Page size",
                    example = "20"
            )
            @RequestParam(value = "size", defaultValue = "20") int size) {
        try {
            List<KycReviewResponse> results;
            if (status != null && !status.isEmpty()) {
                var kycStatus = KycReview.KycStatus.valueOf(status.toUpperCase());
                results = kycReviewService.listByStatus(kycStatus, page, size)
                        .stream()
                        .map(KycReviewResponse::from)
                        .toList();
            } else {
                results = kycReviewService.listAll(page, size)
                        .stream()
                        .map(KycReviewResponse::from)
                        .toList();
            }
            return ok(results);
        } catch (IllegalArgumentException e) {
            return badRequest("Invalid status value: " + status);
        }
    }

    @PostMapping("/kyc-reviews/{id}/review")
    @PreAuthorize("hasAnyAuthority('admin', 'backoffice')")
    @Operation(
            summary = "Submit KYC review decision",
            description = "Submits a review decision for a KYC review. Updates the status and records the reviewer."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "KYC review decision submitted successfully",
                    content = @Content(schema = @Schema(implementation = KycReviewResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "KYC review not found",
                    content = @Content(schema = @Schema(implementation = id.payu.backoffice.dto.ApiResponse.class))
            )
    })
    public ResponseEntity<id.payu.backoffice.dto.ApiResponse<KycReviewResponse>> reviewKyc(
            @Parameter(
                    description = "KYC review ID",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathVariable("id") UUID id,
            @Valid
            @Parameter(
                    description = "KYC review decision",
                    required = true,
                    content = @Content(schema = @Schema(implementation = KycReviewDecisionRequest.class))
            )
            @RequestBody KycReviewDecisionRequest request,
            @Parameter(
                    description = "Admin user ID for audit trail",
                    example = "admin-123"
            )
            @RequestHeader(value = "X-Admin-User", required = false) String adminUser) {
        var resolvedAdmin = resolveAdminUser(adminUser);
        var review = kycReviewService.review(id, request, resolvedAdmin);
        return ok(KycReviewResponse.from(review));
    }

    @DeleteMapping("/kyc-reviews/{id}")
    @PreAuthorize("hasAnyAuthority('admin')")
    @Operation(
            summary = "Delete KYC review",
            description = "Deletes a KYC review. This operation is restricted to administrators only."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "KYC review deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "KYC review not found",
                    content = @Content(schema = @Schema(implementation = id.payu.backoffice.dto.ApiResponse.class))
            )
    })
    public ResponseEntity<Void> deleteKycReview(
            @Parameter(
                    description = "KYC review ID",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathVariable("id") UUID id) {
        kycReviewService.delete(id);
        return noContent();
    }

    // ==================== Fraud Case Endpoints ====================

    @PostMapping(value = "/fraud-cases", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @PreAuthorize("hasAnyAuthority('admin', 'backoffice')")
    @Operation(
            summary = "Create fraud case",
            description = "Creates a new fraud case for investigation. Used to flag suspicious transactions or activities."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Fraud case created successfully",
                    content = @Content(schema = @Schema(implementation = FraudCaseResponse.class)),
                    headers = @Header(
                            name = "Location",
                            description = "URL of the newly created fraud case",
                            schema = @Schema(type = "string")
                    )
            )
    })
    public ResponseEntity<id.payu.backoffice.dto.ApiResponse<FraudCaseResponse>> createFraudCase(
            @Parameter(description = "User ID", example = "user-123", required = true)
            @RequestParam("userId") String userId,
            @Parameter(description = "Account number", example = "1234567890")
            @RequestParam(value = "accountNumber", required = false) String accountNumber,
            @Parameter(description = "Transaction ID (UUID)", example = "123e4567-e89b-12d3-a456-426614174000")
            @RequestParam(value = "transactionId", required = false) String transactionId,
            @Parameter(description = "Transaction type", example = "TRANSFER")
            @RequestParam(value = "transactionType", required = false) String transactionType,
            @Parameter(description = "Transaction amount", example = "1000000.00")
            @RequestParam(value = "amount", required = false) BigDecimal amount,
            @Parameter(description = "Type of fraud", example = "account_takeover", required = true)
            @RequestParam("fraudType") String fraudType,
            @Parameter(description = "Risk level (LOW, MEDIUM, HIGH, CRITICAL)", example = "HIGH")
            @RequestParam(value = "riskLevel", required = false) String riskLevel,
            @Parameter(description = "Description of the fraud case", example = "Suspicious login from unusual location")
            @RequestParam(value = "description", required = false) String description,
            @Parameter(description = "Evidence in JSON format", example = "{\"ip\": \"192.168.1.1\", \"device\": \"unknown\"}")
            @RequestParam(value = "evidence", required = false) String evidence) {
        var risk = riskLevel != null ? FraudCase.RiskLevel.valueOf(riskLevel.toUpperCase()) : FraudCase.RiskLevel.MEDIUM;
        var txId = transactionId != null ? UUID.fromString(transactionId) : null;
        var fraudCase = fraudCaseService.create(userId, accountNumber, txId, transactionType,
                amount, fraudType, risk, description, evidence);
        return created(FraudCaseResponse.from(fraudCase), "/api/v1/backoffice/fraud-cases/" + fraudCase.getId());
    }

    @GetMapping("/fraud-cases/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'backoffice')")
    @Operation(summary = "Get fraud case by ID", description = "Retrieves details of a specific fraud case.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = FraudCaseResponse.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = id.payu.backoffice.dto.ApiResponse.class)))
    })
    public ResponseEntity<id.payu.backoffice.dto.ApiResponse<FraudCaseResponse>> getFraudCase(@PathVariable("id") UUID id) {
        return fraudCaseService.getById(id)
                .map(fraudCase -> ok(FraudCaseResponse.from(fraudCase)))
                .orElse(notFound("Fraud case"));
    }

    @GetMapping("/fraud-cases")
    @PreAuthorize("hasAnyAuthority('admin', 'backoffice')")
    @Operation(summary = "List fraud cases", description = "Retrieves a paginated list of fraud cases.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = FraudCaseResponse.class)))
    })
    public ResponseEntity<id.payu.backoffice.dto.ApiResponse<List<FraudCaseResponse>>> listFraudCases(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "riskLevel", required = false) String riskLevel,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        try {
            List<FraudCaseResponse> results;
            if (riskLevel != null && !riskLevel.isEmpty()) {
                var risk = FraudCase.RiskLevel.valueOf(riskLevel.toUpperCase());
                results = fraudCaseService.listByRiskLevel(risk, page, size).stream().map(FraudCaseResponse::from).toList();
            } else if (status != null && !status.isEmpty()) {
                var caseStatus = FraudCase.CaseStatus.valueOf(status.toUpperCase());
                results = fraudCaseService.listByStatus(caseStatus, page, size).stream().map(FraudCaseResponse::from).toList();
            } else {
                results = fraudCaseService.listAll(page, size).stream().map(FraudCaseResponse::from).toList();
            }
            return ok(results);
        } catch (IllegalArgumentException e) {
            return badRequest("Invalid filter value: " + e.getMessage());
        }
    }

    @PostMapping(value = "/fraud-cases/{id}/assign", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @PreAuthorize("hasAnyAuthority('admin', 'backoffice')")
    @Operation(summary = "Assign fraud case", description = "Assigns a fraud case to a specific investigator.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = FraudCaseResponse.class)))
    })
    public ResponseEntity<id.payu.backoffice.dto.ApiResponse<FraudCaseResponse>> assignFraudCase(
            @PathVariable("id") UUID id,
            @RequestParam("assignedTo") String assignedTo) {
        var fraudCase = fraudCaseService.assign(id, assignedTo);
        return ok(FraudCaseResponse.from(fraudCase));
    }

    @PostMapping("/fraud-cases/{id}/resolve")
    @PreAuthorize("hasAnyAuthority('admin', 'backoffice')")
    @Operation(summary = "Resolve fraud case", description = "Resolves a fraud case with a final decision.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = FraudCaseResponse.class)))
    })
    public ResponseEntity<id.payu.backoffice.dto.ApiResponse<FraudCaseResponse>> resolveFraudCase(
            @PathVariable("id") UUID id,
            @Valid @RequestBody FraudCaseDecisionRequest request,
            @RequestHeader(value = "X-Admin-User", required = false) String adminUser) {
        var resolvedAdmin = resolveAdminUser(adminUser);
        var fraudCase = fraudCaseService.resolve(id, request, resolvedAdmin);
        return ok(FraudCaseResponse.from(fraudCase));
    }

    @DeleteMapping("/fraud-cases/{id}")
    @PreAuthorize("hasAnyAuthority('admin')")
    @Operation(summary = "Delete fraud case", description = "Deletes a fraud case.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Fraud case deleted successfully")
    })
    public ResponseEntity<Void> deleteFraudCase(@PathVariable("id") UUID id) {
        fraudCaseService.delete(id);
        return noContent();
    }

    // ==================== Customer Case Endpoints ====================

    @PostMapping("/customer-cases")
    @PreAuthorize("hasAnyAuthority('admin', 'backoffice')")
    @Operation(summary = "Create customer case", description = "Creates a new customer support case.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = CustomerCaseResponse.class)))
    })
    public ResponseEntity<id.payu.backoffice.dto.ApiResponse<CustomerCaseResponse>> createCustomerCase(
            @Valid @RequestBody CustomerCaseRequest request) {
        var customerCase = customerCaseService.create(request);
        return created(CustomerCaseResponse.from(customerCase), "/api/v1/backoffice/customer-cases/" + customerCase.getId());
    }

    @GetMapping("/customer-cases/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'backoffice')")
    @Operation(summary = "Get customer case by ID", description = "Retrieves details of a specific customer support case.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CustomerCaseResponse.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = id.payu.backoffice.dto.ApiResponse.class)))
    })
    public ResponseEntity<id.payu.backoffice.dto.ApiResponse<CustomerCaseResponse>> getCustomerCase(@PathVariable("id") UUID id) {
        return customerCaseService.getById(id)
                .map(customerCase -> ok(CustomerCaseResponse.from(customerCase)))
                .orElse(notFound("Customer case"));
    }

    @GetMapping("/customer-cases")
    @PreAuthorize("hasAnyAuthority('admin', 'backoffice')")
    @Operation(summary = "List customer cases", description = "Retrieves a paginated list of customer support cases.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CustomerCaseResponse.class)))
    })
    public ResponseEntity<id.payu.backoffice.dto.ApiResponse<List<CustomerCaseResponse>>> listCustomerCases(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        try {
            List<CustomerCaseResponse> results;
            if (priority != null && !priority.isEmpty()) {
                var prio = CustomerCase.Priority.valueOf(priority.toUpperCase());
                results = customerCaseService.listByPriority(prio, page, size).stream().map(CustomerCaseResponse::from).toList();
            } else if (status != null && !status.isEmpty()) {
                var caseStatus = CustomerCase.CaseStatus.valueOf(status.toUpperCase());
                results = customerCaseService.listByStatus(caseStatus, page, size).stream().map(CustomerCaseResponse::from).toList();
            } else {
                results = customerCaseService.listAll(page, size).stream().map(CustomerCaseResponse::from).toList();
            }
            return ok(results);
        } catch (IllegalArgumentException e) {
            return badRequest("Invalid filter value: " + e.getMessage());
        }
    }

    @PostMapping(value = "/customer-cases/{id}/assign", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @PreAuthorize("hasAnyAuthority('admin', 'backoffice')")
    @Operation(summary = "Assign customer case", description = "Assigns a customer support case to a specific agent.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CustomerCaseResponse.class)))
    })
    public ResponseEntity<id.payu.backoffice.dto.ApiResponse<CustomerCaseResponse>> assignCustomerCase(
            @PathVariable("id") UUID id,
            @RequestParam("assignedTo") String assignedTo) {
        var customerCase = customerCaseService.assign(id, assignedTo);
        return ok(CustomerCaseResponse.from(customerCase));
    }

    @PutMapping("/customer-cases/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'backoffice')")
    @Operation(summary = "Update customer case", description = "Updates a customer support case with new status and notes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CustomerCaseResponse.class)))
    })
    public ResponseEntity<id.payu.backoffice.dto.ApiResponse<CustomerCaseResponse>> updateCustomerCase(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CustomerCaseUpdateRequest request,
            @RequestHeader(value = "X-Admin-User", required = false) String adminUser) {
        var resolvedAdmin = resolveAdminUser(adminUser);
        var customerCase = customerCaseService.update(id, request, resolvedAdmin);
        return ok(CustomerCaseResponse.from(customerCase));
    }

    @DeleteMapping("/customer-cases/{id}")
    @PreAuthorize("hasAnyAuthority('admin')")
    @Operation(summary = "Delete customer case", description = "Deletes a customer support case.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Customer case deleted successfully")
    })
    public ResponseEntity<Void> deleteCustomerCase(@PathVariable("id") UUID id) {
        customerCaseService.delete(id);
        return noContent();
    }

    // ==================== Universal Search Endpoints ====================

    @PostMapping("/search")
    @PreAuthorize("hasAnyAuthority('admin', 'backoffice')")
    @Operation(summary = "Universal search (POST)", description = "Performs a universal search across multiple entities.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = UniversalSearchResponse.class)))
    })
    public ResponseEntity<id.payu.backoffice.dto.ApiResponse<UniversalSearchResponse>> search(
            @Valid @RequestBody UniversalSearchRequest request) {
        LOG.info("Universal search request: query={}, entityType={}", request.query(), request.entityType());
        var results = universalSearchService.search(
                request.query(),
                request.entityType(),
                request.page(),
                request.size()
        );
        return ok(results);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('admin', 'backoffice')")
    @Operation(summary = "Universal search (GET)", description = "Performs a universal search across multiple entities using query parameters.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = UniversalSearchResponse.class)))
    })
    public ResponseEntity<id.payu.backoffice.dto.ApiResponse<UniversalSearchResponse>> searchGet(
            @RequestParam("q") String query,
            @RequestParam(value = "type", required = false) String entityType,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        LOG.info("Universal search GET request: query={}, entityType={}", query, entityType);
        if (query == null || query.isEmpty()) {
            return badRequest("Query parameter 'q' is required");
        }
        var results = universalSearchService.search(query, entityType, page, size);
        return ok(results);
    }
}
