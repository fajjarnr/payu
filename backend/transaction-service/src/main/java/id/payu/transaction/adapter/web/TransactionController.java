package id.payu.transaction.adapter.web;

import id.payu.api.common.constant.ApiConstants;
import id.payu.api.common.constant.ErrorCode;
import id.payu.api.common.controller.BaseController;
import id.payu.api.common.controller.RateLimit;
import id.payu.api.common.exception.BusinessException;
import id.payu.api.common.openapi.FilterParameter;
import id.payu.api.common.openapi.OpenApiConstants;
import id.payu.api.common.openapi.PaginationParameter;
import id.payu.api.common.response.ApiResponse;
import id.payu.api.common.response.PaginationInfo;
import id.payu.commons.idempotency.Idempotent;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommand;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommandResult;
import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.in.TransactionUseCase;
import id.payu.transaction.dto.InitiateTransferRequest;
import id.payu.transaction.dto.InitiateTransferResponse;
import id.payu.transaction.dto.ProcessQrisPaymentRequest;
import id.payu.transaction.dto.TransactionResponse;
import id.payu.transaction.dto.UpdateTransactionTagsRequest;
import id.payu.security.annotation.Audited;
import id.payu.security.annotation.Audited.AuditLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Transaction controller for PayU Digital Banking Platform.
 * Handles fund transfers, QRIS payments, and transaction queries.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = OpenApiConstants.Tags.TRANSACTIONS, description = "Fund transfer and payment endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController extends BaseController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TransactionController.class);



    private final TransactionUseCase transactionUseCase;

    public TransactionController(TransactionUseCase transactionUseCase) {
        this.transactionUseCase = transactionUseCase;
    }

    /**
     * Extracts the user ID from the JWT authentication token.
     * BUG-AUTH-013: Standardized to use 'account_id' claim with 'sub' fallback.
     *
     * @return The user ID from the JWT
     * @throws IllegalStateException if no authentication is present
     */
    private String extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            throw new IllegalStateException("No valid JWT authentication found");
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String accountId = jwt.getClaimAsString("account_id");
        return accountId != null ? accountId : jwt.getSubject();
    }

    /**
     * Initiate a fund transfer to another account.
     * Supports BI-FAST, SKN, and internal transfers.
     */
    @PostMapping("/transfer")
    @Audited(
            operation = id.payu.security.annotation.Audited.Operation.TRANSFER,
            entityType = "Transaction",
            maskData = true,
            level = AuditLevel.INFO
    )
    @Operation(
            summary = "Initiate fund transfer",
            description = """
                    Initiates a fund transfer from sender's account to recipient account.

                    **Transfer Types:**
                    - `BI_FAST`: Instant transfer up to Rp 250 million (default)
                    - `SKN`: Same-day transfer for amounts above Rp 250 million
                    - `INTERNAL`: Instant transfer between PayU accounts

                    **Rate Limiting:** 100 requests per minute

                    **Idempotency:** Use `Idempotency-Key` header to prevent duplicate transfers
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Transfer initiated successfully",
                    content = @Content(schema = @Schema(implementation = InitiateTransferResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request | Insufficient balance",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "Business rule violation",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @RateLimit(requests = 100, windowSeconds = 60, keyPrefix = "transfer")
    @Idempotent(required = true)
    @PreAuthorize("hasAuthority('write:transaction')")
    public ResponseEntity<ApiResponse<InitiateTransferResponse>> initiateTransfer(
            @Valid @RequestBody InitiateTransferRequest request
    ) {
        try {
            String userId = extractUserId();
            InitiateTransferCommandResult result = transactionUseCase.initiateTransfer(request, userId);
            InitiateTransferResponse response = result.toResponse();
            return created(response, "/api/v1/transactions/" + result.transactionId());
        } catch (BusinessException e) {
            log.warn("Transfer initiation failed: {}", e.getMessage());
            return ResponseEntity.unprocessableEntity()
                    .body(ApiResponse.error(e.getCode(), e.getMessage()));
        }
        // BUG-BE-144: Removed generic Exception catch — GlobalExceptionHandler handles unexpected errors uniformly
    }

    /**
     * Get transaction details by transaction ID.
     */
    @GetMapping("/{transactionId}")
    @Operation(
            summary = "Get transaction details",
            description = "Retrieves detailed information about a specific transaction."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Transaction found",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Transaction not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PreAuthorize("hasAuthority('read:transaction')")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @Parameter(description = "Transaction ID", required = true)
            @PathVariable UUID transactionId
    ) {
        try {
            String userId = extractUserId();
            Transaction transaction = transactionUseCase.getTransaction(transactionId, userId);
            // BUG-BE-135 FIX: Return DTO instead of domain entity
            return ok(TransactionResponse.from(transaction));
        } catch (BusinessException e) {
            log.warn("Transaction not found: {}", transactionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getCode(), e.getMessage()));
        }
    }

    /**
     * Get list of transactions for an account with pagination.
     */
    @GetMapping("/accounts/{accountId}")
    @Operation(
            summary = "List account transactions",
            description = """
                    Retrieves a paginated list of transactions for the specified account.

                    **Default Sorting:** createdAt,desc (newest first)

                    **Filters:**
                    - status: Filter by transaction status (COMPLETED, PENDING, FAILED)
                    - startDate: Filter by start date (ISO 8601 format)
                    - endDate: Filter by end date (ISO 8601 format)
                    - type: Filter by transaction type (TRANSFER, QRIS, BILL_PAYMENT)
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Transactions retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            )
    })
    @PreAuthorize("hasAuthority('read:transaction')")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAccountTransactions(
            @Parameter(description = "Account ID", required = true)
            @PathVariable UUID accountId,

            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort field and direction (e.g., createdAt,desc)", example = "amount,asc")
            @RequestParam(required = false) String sort,

            @Parameter(description = "Filter by transaction status")
            @RequestParam(required = false) String status,

            @Parameter(description = "Filter by start date (ISO 8601)")
            @RequestParam(required = false) String startDate,

            @Parameter(description = "Filter by end date (ISO 8601)")
            @RequestParam(required = false) String endDate
    ) {
        try {
            String userId = extractUserId();

            // BUG-BE-136: Verify the authenticated user owns this accountId
            // The UseCase handles the ownership validation via userId parameter,
            // but we add an explicit log for audit trail.
            log.info("User {} requesting transactions for account {}", userId, accountId);

            // Create pageable from parameters
            var pageable = createPageable(page, size, sort, ApiConstants.DEFAULT_SORT_DIRECTION);

            // Get transactions (UseCase validates userId ownership)
            List<Transaction> transactions = transactionUseCase.getAccountTransactions(
                    accountId,
                    userId,
                    pageable.getPageNumber(),
                    pageable.getPageSize()
            );

            // BUG-BE-135 + BUG-BE-137 FIX: Map to DTOs and include pagination info
            List<TransactionResponse> responseList = transactions.stream()
                    .map(TransactionResponse::from)
                    .toList();
            PaginationInfo pagination = PaginationInfo.builder()
                    .page(pageable.getPageNumber())
                    .size(pageable.getPageSize())
                    .totalElements((long) transactions.size())
                    .totalPages(transactions.size() < pageable.getPageSize() ? 1 :
                            (int) Math.ceil((double) transactions.size() / pageable.getPageSize()))
                    .hasNext(transactions.size() >= pageable.getPageSize())
                    .hasPrevious(pageable.getPageNumber() > 0)
                    .build();
            return ResponseEntity.ok(ApiResponse.success(responseList, pagination));
        } catch (BusinessException e) {
            log.warn("Error retrieving transactions for account: {} - {}", accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getCode(), e.getMessage()));
        }
        // BUG-BE-144: Removed generic Exception catch — GlobalExceptionHandler handles unexpected errors uniformly
    }

    /**
     * Process QRIS payment.
     */
    @PostMapping("/qris/pay")
    @Audited(
            operation = id.payu.security.annotation.Audited.Operation.TRANSFER,
            entityType = "Transaction",
            maskData = true,
            level = AuditLevel.INFO
    )
    @Operation(
            summary = "Process QRIS payment",
            description = """
                    Processes a payment through QRIS (Quick Response Code Indonesian Standard).

                    **Rate Limiting:** 100 requests per minute

                    **Idempotency:** Use `Idempotency-Key` header to prevent duplicate payments
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "QRIS payment accepted for processing"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid QR code | Expired QR",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "Insufficient balance | Business rule violation",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @RateLimit(requests = 100, windowSeconds = 60, keyPrefix = "qris")
    @Idempotent(required = true)
    @PreAuthorize("hasAuthority('write:payment')")
    public ResponseEntity<ApiResponse<Void>> processQrisPayment(
            @Valid @RequestBody ProcessQrisPaymentRequest request
    ) {
        try {
            String userId = extractUserId();
            transactionUseCase.processQrisPayment(request, userId);
            return ResponseEntity.accepted()
                    .body(ApiResponse.<Void>success(null));
        } catch (BusinessException e) {
            log.warn("QRIS payment failed: {}", e.getMessage());
            return ResponseEntity.unprocessableEntity()
                    .body(ApiResponse.error(e.getCode(), e.getMessage()));
        }
        // BUG-BE-144: Removed generic Exception catch — GlobalExceptionHandler handles unexpected errors uniformly
    }

    /**
     * Update transaction tags (IMP-037).
     */
    @PatchMapping("/{transactionId}/tags")
    @Operation(
            summary = "Update transaction tags",
            description = """
                    Updates the tags for a specific transaction.
                    Supports predefined categories and custom tags.
                    Maximum 10 tags per transaction.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Tags updated successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Transaction not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PreAuthorize("hasAuthority('write:transaction')")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransactionTags(
            @Parameter(description = "Transaction ID", required = true)
            @PathVariable UUID transactionId,
            @Valid @RequestBody UpdateTransactionTagsRequest request
    ) {
        try {
            String userId = extractUserId();
            Transaction transaction = transactionUseCase.updateTransactionTags(transactionId, userId, request.getTags());
            return ok(TransactionResponse.from(transaction));
        } catch (BusinessException e) {
            log.warn("Failed to update transaction tags: {} - {}", transactionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getCode(), e.getMessage()));
        }
    }

    @Override
    protected String getBaseUrl() {
        return "/api/v1/transactions";
    }
}
