package id.payu.backoffice.resource;

import id.payu.backoffice.domain.CustomerCase;
import id.payu.backoffice.domain.FraudCase;
import id.payu.backoffice.domain.KycReview;
import id.payu.backoffice.dto.*;
import id.payu.backoffice.service.CustomerCaseService;
import id.payu.backoffice.service.FraudCaseService;
import id.payu.backoffice.service.KycReviewService;
import id.payu.backoffice.service.UniversalSearchService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

/**
 * REST resource for backoffice operations including KYC reviews, fraud case management,
 * customer case management, and universal search.
 */
@Path("/api/v1/backoffice")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Backoffice Operations", description = "Internal management operations for KYC review, fraud monitoring, and customer support")
@SecurityRequirement(name = "bearerAuth")
public class BackofficeResource extends BackofficeBaseResource {

    @Inject
    KycReviewService kycReviewService;

    @Inject
    FraudCaseService fraudCaseService;

    @Inject
    CustomerCaseService customerCaseService;

    @Inject
    UniversalSearchService universalSearchService;

    // ==================== KYC Review Endpoints ====================

    @POST
    @Path("/kyc-reviews")
    @RolesAllowed({"admin", "backoffice"})
    @Operation(
            summary = "Create KYC review",
            description = "Creates a new KYC (Know Your Customer) review for manual verification. " +
                    "This initiates the review process for customer identity verification."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "201",
                    description = "KYC review created successfully",
                    content = @Content(schema = @Schema(implementation = KycReviewResponse.class)),
                    headers = @Header(
                            name = "Location",
                            description = "URL of the newly created KYC review",
                            schema = @Schema(type = SchemaType.STRING)
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public Response createKycReview(
            @Valid
            @Parameter(
                    description = "KYC review request details",
                    required = true,
                    content = @Content(schema = @Schema(implementation = KycReviewRequest.class))
            )
            KycReviewRequest request) {
        LOG.infof("Creating KYC review for user: %s", request.userId());
        var review = kycReviewService.create(request);
        return created(KycReviewResponse.from(review), "/api/v1/backoffice/kyc-reviews/" + review.id);
    }

    @GET
    @Path("/kyc-reviews/{id}")
    @RolesAllowed({"admin", "backoffice"})
    @Operation(
            summary = "Get KYC review by ID",
            description = "Retrieves details of a specific KYC review including current status and review history."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "KYC review retrieved successfully",
                    content = @Content(schema = @Schema(implementation = KycReviewResponse.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "KYC review not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public Response getKycReview(
            @Parameter(
                    description = "KYC review ID",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathParam("id") UUID id) {
        return kycReviewService.getById(id)
                .map(review -> ok(KycReviewResponse.from(review)))
                .orElse(notFound("KYC review"));
    }

    @GET
    @Path("/kyc-reviews")
    @RolesAllowed({"admin", "backoffice"})
    @Operation(
            summary = "List KYC reviews",
            description = "Retrieves a paginated list of KYC reviews. Can be filtered by status."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "List of KYC reviews",
                    content = @Content(schema = @Schema(implementation = KycReviewResponse.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public List<KycReviewResponse> listKycReviews(
            @Parameter(
                    description = "Filter by status (PENDING, UNDER_REVIEW, APPROVED, REJECTED, REQUIRES_ADDITIONAL_INFO)",
                    example = "PENDING"
            )
            @QueryParam("status") String status,
            @Parameter(
                    description = "Page number (0-based)",
                    example = "0"
            )
            @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(
                    description = "Page size",
                    example = "20"
            )
            @QueryParam("size") @DefaultValue("20") int size) {
        if (status != null && !status.isEmpty()) {
            var kycStatus = KycReview.KycStatus.valueOf(status.toUpperCase());
            return kycReviewService.listByStatus(kycStatus, page, size)
                    .stream()
                    .map(KycReviewResponse::from)
                    .toList();
        }
        return kycReviewService.listAll(page, size)
                .stream()
                .map(KycReviewResponse::from)
                .toList();
    }

    @POST
    @Path("/kyc-reviews/{id}/review")
    @RolesAllowed({"admin", "backoffice"})
    @Operation(
            summary = "Submit KYC review decision",
            description = "Submits a review decision for a KYC review. Updates the status and records the reviewer."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "KYC review decision submitted successfully",
                    content = @Content(schema = @Schema(implementation = KycReviewResponse.class))
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "KYC review not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public Response reviewKyc(
            @Parameter(
                    description = "KYC review ID",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathParam("id") UUID id,
            @Valid
            @Parameter(
                    description = "KYC review decision",
                    required = true,
                    content = @Content(schema = @Schema(implementation = KycReviewDecisionRequest.class))
            )
            KycReviewDecisionRequest request,
            @Parameter(
                    description = "Admin user ID for audit trail",
                    example = "admin-123"
            )
            @HeaderParam("X-Admin-User") String adminUser) {
        if (adminUser == null || adminUser.isEmpty()) {
            adminUser = "system";
        }
        var review = kycReviewService.review(id, request, adminUser);
        return ok(KycReviewResponse.from(review));
    }

    @DELETE
    @Path("/kyc-reviews/{id}")
    @RolesAllowed({"admin"})
    @Operation(
            summary = "Delete KYC review",
            description = "Deletes a KYC review. This operation is restricted to administrators only."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "204",
                    description = "KYC review deleted successfully"
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "KYC review not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public Response deleteKycReview(
            @Parameter(
                    description = "KYC review ID",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathParam("id") UUID id) {
        kycReviewService.delete(id);
        return noContent();
    }

    // ==================== Fraud Case Endpoints ====================

    @POST
    @Path("/fraud-cases")
    @RolesAllowed({"admin", "backoffice"})
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
            summary = "Create fraud case",
            description = "Creates a new fraud case for investigation. Used to flag suspicious transactions or activities."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "201",
                    description = "Fraud case created successfully",
                    content = @Content(schema = @Schema(implementation = FraudCaseResponse.class)),
                    headers = @Header(
                            name = "Location",
                            description = "URL of the newly created fraud case",
                            schema = @Schema(type = SchemaType.STRING)
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public Response createFraudCase(
            @Parameter(description = "User ID", example = "user-123", required = true)
            @FormParam("userId") String userId,
            @Parameter(description = "Account number", example = "1234567890")
            @FormParam("accountNumber") String accountNumber,
            @Parameter(description = "Transaction ID (UUID)", example = "123e4567-e89b-12d3-a456-426614174000")
            @FormParam("transactionId") String transactionId,
            @Parameter(description = "Transaction type", example = "TRANSFER")
            @FormParam("transactionType") String transactionType,
            @Parameter(description = "Transaction amount", example = "1000000.00")
            @FormParam("amount") BigDecimal amount,
            @Parameter(description = "Type of fraud", example = "account_takeover", required = true)
            @FormParam("fraudType") String fraudType,
            @Parameter(description = "Risk level (LOW, MEDIUM, HIGH, CRITICAL)", example = "HIGH")
            @FormParam("riskLevel") String riskLevel,
            @Parameter(description = "Description of the fraud case", example = "Suspicious login from unusual location")
            @FormParam("description") String description,
            @Parameter(description = "Evidence in JSON format", example = "{\"ip\": \"192.168.1.1\", \"device\": \"unknown\"}")
            @FormParam("evidence") String evidence) {
        var risk = riskLevel != null ? FraudCase.RiskLevel.valueOf(riskLevel.toUpperCase()) : FraudCase.RiskLevel.MEDIUM;
        var txId = transactionId != null ? UUID.fromString(transactionId) : null;
        var fraudCase = fraudCaseService.create(userId, accountNumber, txId, transactionType,
                amount, fraudType, risk, description, evidence);
        return created(FraudCaseResponse.from(fraudCase), "/api/v1/backoffice/fraud-cases/" + fraudCase.id);
    }

    @GET
    @Path("/fraud-cases/{id}")
    @RolesAllowed({"admin", "backoffice"})
    @Operation(
            summary = "Get fraud case by ID",
            description = "Retrieves details of a specific fraud case including investigation status and resolution."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "Fraud case retrieved successfully",
                    content = @Content(schema = @Schema(implementation = FraudCaseResponse.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Fraud case not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public Response getFraudCase(
            @Parameter(
                    description = "Fraud case ID",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathParam("id") UUID id) {
        return fraudCaseService.getById(id)
                .map(fraudCase -> ok(FraudCaseResponse.from(fraudCase)))
                .orElse(notFound("Fraud case"));
    }

    @GET
    @Path("/fraud-cases")
    @RolesAllowed({"admin", "backoffice"})
    @Operation(
            summary = "List fraud cases",
            description = "Retrieves a paginated list of fraud cases. Can be filtered by status or risk level."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "List of fraud cases",
                    content = @Content(schema = @Schema(implementation = FraudCaseResponse.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public List<FraudCaseResponse> listFraudCases(
            @Parameter(
                    description = "Filter by status (OPEN, UNDER_INVESTIGATION, RESOLVED, CLOSED, ESCALATED)",
                    example = "OPEN"
            )
            @QueryParam("status") String status,
            @Parameter(
                    description = "Filter by risk level (LOW, MEDIUM, HIGH, CRITICAL)",
                    example = "HIGH"
            )
            @QueryParam("riskLevel") String riskLevel,
            @Parameter(
                    description = "Page number (0-based)",
                    example = "0"
            )
            @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(
                    description = "Page size",
                    example = "20"
            )
            @QueryParam("size") @DefaultValue("20") int size) {
        if (riskLevel != null && !riskLevel.isEmpty()) {
            var risk = FraudCase.RiskLevel.valueOf(riskLevel.toUpperCase());
            return fraudCaseService.listByRiskLevel(risk, page, size)
                    .stream()
                    .map(FraudCaseResponse::from)
                    .toList();
        }
        if (status != null && !status.isEmpty()) {
            var caseStatus = FraudCase.CaseStatus.valueOf(status.toUpperCase());
            return fraudCaseService.listByStatus(caseStatus, page, size)
                    .stream()
                    .map(FraudCaseResponse::from)
                    .toList();
        }
        return fraudCaseService.listAll(page, size)
                .stream()
                .map(FraudCaseResponse::from)
                .toList();
    }

    @POST
    @Path("/fraud-cases/{id}/assign")
    @RolesAllowed({"admin", "backoffice"})
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
            summary = "Assign fraud case",
            description = "Assigns a fraud case to a specific investigator or team member."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "Fraud case assigned successfully",
                    content = @Content(schema = @Schema(implementation = FraudCaseResponse.class))
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Fraud case not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public Response assignFraudCase(
            @Parameter(
                    description = "Fraud case ID",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathParam("id") UUID id,
            @Parameter(
                    description = "Assignee user ID",
                    required = true,
                    example = "investigator-123"
            )
            @FormParam("assignedTo") String assignedTo) {
        var fraudCase = fraudCaseService.assign(id, assignedTo);
        return ok(FraudCaseResponse.from(fraudCase));
    }

    @POST
    @Path("/fraud-cases/{id}/resolve")
    @RolesAllowed({"admin", "backoffice"})
    @Operation(
            summary = "Resolve fraud case",
            description = "Resolves a fraud case with a final decision and notes. Records the resolver for audit trail."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "Fraud case resolved successfully",
                    content = @Content(schema = @Schema(implementation = FraudCaseResponse.class))
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Fraud case not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public Response resolveFraudCase(
            @Parameter(
                    description = "Fraud case ID",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathParam("id") UUID id,
            @Valid
            @Parameter(
                    description = "Fraud case resolution decision",
                    required = true,
                    content = @Content(schema = @Schema(implementation = FraudCaseDecisionRequest.class))
            )
            FraudCaseDecisionRequest request,
            @Parameter(
                    description = "Admin user ID for audit trail",
                    example = "admin-123"
            )
            @HeaderParam("X-Admin-User") String adminUser) {
        if (adminUser == null || adminUser.isEmpty()) {
            adminUser = "system";
        }
        var fraudCase = fraudCaseService.resolve(id, request, adminUser);
        return ok(FraudCaseResponse.from(fraudCase));
    }

    @DELETE
    @Path("/fraud-cases/{id}")
    @RolesAllowed({"admin"})
    @Operation(
            summary = "Delete fraud case",
            description = "Deletes a fraud case. This operation is restricted to administrators only."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "204",
                    description = "Fraud case deleted successfully"
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Fraud case not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public Response deleteFraudCase(
            @Parameter(
                    description = "Fraud case ID",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathParam("id") UUID id) {
        fraudCaseService.delete(id);
        return noContent();
    }

    // ==================== Customer Case Endpoints ====================

    @POST
    @Path("/customer-cases")
    @RolesAllowed({"admin", "backoffice"})
    @Operation(
            summary = "Create customer case",
            description = "Creates a new customer support case for handling customer inquiries and issues."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "201",
                    description = "Customer case created successfully",
                    content = @Content(schema = @Schema(implementation = CustomerCaseResponse.class)),
                    headers = @Header(
                            name = "Location",
                            description = "URL of the newly created customer case",
                            schema = @Schema(type = SchemaType.STRING)
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public Response createCustomerCase(
            @Valid
            @Parameter(
                    description = "Customer case request details",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CustomerCaseRequest.class))
            )
            CustomerCaseRequest request) {
        var customerCase = customerCaseService.create(request);
        return created(CustomerCaseResponse.from(customerCase), "/api/v1/backoffice/customer-cases/" + customerCase.id);
    }

    @GET
    @Path("/customer-cases/{id}")
    @RolesAllowed({"admin", "backoffice"})
    @Operation(
            summary = "Get customer case by ID",
            description = "Retrieves details of a specific customer support case including status and resolution."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "Customer case retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CustomerCaseResponse.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Customer case not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public Response getCustomerCase(
            @Parameter(
                    description = "Customer case ID",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathParam("id") UUID id) {
        return customerCaseService.getById(id)
                .map(customerCase -> ok(CustomerCaseResponse.from(customerCase)))
                .orElse(notFound("Customer case"));
    }

    @GET
    @Path("/customer-cases")
    @RolesAllowed({"admin", "backoffice"})
    @Operation(
            summary = "List customer cases",
            description = "Retrieves a paginated list of customer support cases. Can be filtered by status or priority."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "List of customer cases",
                    content = @Content(schema = @Schema(implementation = CustomerCaseResponse.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public List<CustomerCaseResponse> listCustomerCases(
            @Parameter(
                    description = "Filter by status (OPEN, IN_PROGRESS, RESOLVED, CLOSED, ESCALATED)",
                    example = "OPEN"
            )
            @QueryParam("status") String status,
            @Parameter(
                    description = "Filter by priority (LOW, MEDIUM, HIGH, URGENT)",
                    example = "HIGH"
            )
            @QueryParam("priority") String priority,
            @Parameter(
                    description = "Page number (0-based)",
                    example = "0"
            )
            @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(
                    description = "Page size",
                    example = "20"
            )
            @QueryParam("size") @DefaultValue("20") int size) {
        if (priority != null && !priority.isEmpty()) {
            var prio = CustomerCase.Priority.valueOf(priority.toUpperCase());
            return customerCaseService.listByPriority(prio, page, size)
                    .stream()
                    .map(CustomerCaseResponse::from)
                    .toList();
        }
        if (status != null && !status.isEmpty()) {
            var caseStatus = CustomerCase.CaseStatus.valueOf(status.toUpperCase());
            return customerCaseService.listByStatus(caseStatus, page, size)
                    .stream()
                    .map(CustomerCaseResponse::from)
                    .toList();
        }
        return customerCaseService.listAll(page, size)
                .stream()
                .map(CustomerCaseResponse::from)
                .toList();
    }

    @POST
    @Path("/customer-cases/{id}/assign")
    @RolesAllowed({"admin", "backoffice"})
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(
            summary = "Assign customer case",
            description = "Assigns a customer support case to a specific agent or team member."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "Customer case assigned successfully",
                    content = @Content(schema = @Schema(implementation = CustomerCaseResponse.class))
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Customer case not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public Response assignCustomerCase(
            @Parameter(
                    description = "Customer case ID",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathParam("id") UUID id,
            @Parameter(
                    description = "Assignee user ID",
                    required = true,
                    example = "agent-123"
            )
            @FormParam("assignedTo") String assignedTo) {
        var customerCase = customerCaseService.assign(id, assignedTo);
        return ok(CustomerCaseResponse.from(customerCase));
    }

    @PUT
    @Path("/customer-cases/{id}")
    @RolesAllowed({"admin", "backoffice"})
    @Operation(
            summary = "Update customer case",
            description = "Updates a customer support case with new status and notes. Records the updater for audit trail."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "Customer case updated successfully",
                    content = @Content(schema = @Schema(implementation = CustomerCaseResponse.class))
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Customer case not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public Response updateCustomerCase(
            @Parameter(
                    description = "Customer case ID",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathParam("id") UUID id,
            @Valid
            @Parameter(
                    description = "Customer case update details",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CustomerCaseUpdateRequest.class))
            )
            CustomerCaseUpdateRequest request,
            @Parameter(
                    description = "Admin user ID for audit trail",
                    example = "admin-123"
            )
            @HeaderParam("X-Admin-User") String adminUser) {
        if (adminUser == null || adminUser.isEmpty()) {
            adminUser = "system";
        }
        var customerCase = customerCaseService.update(id, request, adminUser);
        return ok(CustomerCaseResponse.from(customerCase));
    }

    @DELETE
    @Path("/customer-cases/{id}")
    @RolesAllowed({"admin"})
    @Operation(
            summary = "Delete customer case",
            description = "Deletes a customer support case. This operation is restricted to administrators only."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "204",
                    description = "Customer case deleted successfully"
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Customer case not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public Response deleteCustomerCase(
            @Parameter(
                    description = "Customer case ID",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathParam("id") UUID id) {
        customerCaseService.delete(id);
        return noContent();
    }

    // ==================== Universal Search Endpoints ====================

    @POST
    @Path("/search")
    @RolesAllowed({"admin", "backoffice"})
    @Operation(
            summary = "Universal search (POST)",
            description = "Performs a universal search across multiple entities (KYC reviews, fraud cases, customer cases). " +
                    "POST method allows complex search criteria in request body."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "Search completed successfully",
                    content = @Content(schema = @Schema(implementation = UniversalSearchResponse.class))
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public Response search(
            @Valid
            @Parameter(
                    description = "Universal search request",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UniversalSearchRequest.class))
            )
            UniversalSearchRequest request) {
        LOG.infof("Universal search request: query=%s, entityType=%s", request.query(), request.entityType());
        var results = universalSearchService.search(
                request.query(),
                request.entityType(),
                request.page(),
                request.size()
        );
        return ok(results);
    }

    @GET
    @Path("/search")
    @RolesAllowed({"admin", "backoffice"})
    @Operation(
            summary = "Universal search (GET)",
            description = "Performs a universal search across multiple entities using query parameters. " +
                    "GET method for simple searches."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "Search completed successfully",
                    content = @Content(schema = @Schema(implementation = UniversalSearchResponse.class))
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Missing required query parameter",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public Response searchGet(
            @Parameter(
                    description = "Search query string",
                    required = true,
                    example = "John Doe"
            )
            @QueryParam("q") String query,
            @Parameter(
                    description = "Entity type to search (kyc_reviews, fraud_cases, customer_cases)",
                    example = "customer_cases"
            )
            @QueryParam("type") String entityType,
            @Parameter(
                    description = "Page number (0-based)",
                    example = "0"
            )
            @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(
                    description = "Page size",
                    example = "20"
            )
            @QueryParam("size") @DefaultValue("20") int size) {
        LOG.infof("Universal search GET request: query=%s, entityType=%s", query, entityType);
        if (query == null || query.isEmpty()) {
            return badRequest("Query parameter 'q' is required");
        }
        var results = universalSearchService.search(query, entityType, page, size);
        return ok(results);
    }
}
