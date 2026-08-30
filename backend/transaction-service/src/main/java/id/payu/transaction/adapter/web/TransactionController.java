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
import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.domain.port.in.TransactionUseCase;
import id.payu.transaction.domain.port.out.StepUpVerificationPort;
import id.payu.transaction.interfaces.dto.InitiateTransferRequest;
import id.payu.transaction.interfaces.dto.InitiateTransferResponse;
import id.payu.transaction.interfaces.dto.InterbankTransferCallbackRequest;
import id.payu.transaction.interfaces.dto.ProcessQrisPaymentRequest;
import id.payu.transaction.interfaces.dto.TransactionResponse;
import id.payu.transaction.interfaces.dto.TransactionRefundDetailsResponse;
import id.payu.transaction.interfaces.dto.TransactionSummaryResponse;
import id.payu.transaction.interfaces.dto.UpdateTransactionTagsRequest;
import id.payu.security.annotation.Audited;
import id.payu.security.annotation.AuditLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
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
import id.payu.security.annotation.AuditOperation;

/**
 * TransactionEntity controller for PayU Digital Banking Platform.
 * Handles fund transfers, QRIS payments, and transaction queries.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = OpenApiConstants.Tags.TRANSACTIONS, description = "Fund transfer and payment endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController extends BaseController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TransactionController.class);



    private final TransactionUseCase transactionUseCase;
    private final id.payu.transaction.application.service.AccountTransactionSummaryService accountTransactionSummaryService;
    private final StepUpVerificationPort stepUpVerificationPort;

    public TransactionController(TransactionUseCase transactionUseCase,
                                 id.payu.transaction.application.service.AccountTransactionSummaryService accountTransactionSummaryService,
                                 StepUpVerificationPort stepUpVerificationPort) {
        this.transactionUseCase = transactionUseCase;
        this.accountTransactionSummaryService = accountTransactionSummaryService;
        this.stepUpVerificationPort = stepUpVerificationPort;
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
     * List transactions for the authenticated user or by accountId query parameter.
     */
    @GetMapping
    @Operation(
            summary = "List transactions",
            description = "Retrieves paginated transaction list. Use accountId query param to filter by account."
    )
    @PreAuthorize("hasAuthority('read:transaction')")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> listTransactions(
            @Parameter(description = "Account ID to filter transactions")
            @RequestParam(required = false) UUID accountId,

            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page (max 100)", example = "20")
            @RequestParam(defaultValue = "20") @Max(value = 100, message = "Page size must not exceed 100") int size,

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
            UUID effectiveAccountId = accountId != null ? accountId : UUID.fromString(userId);
            var pageable = createPageable(page, size, sort, ApiConstants.DEFAULT_SORT_DIRECTION);
            List<TransactionEntity> transactions = transactionUseCase.getAccountTransactions(
                    effectiveAccountId, userId, pageable.getPageNumber(), pageable.getPageSize());
            List<TransactionResponse> responseList = transactions.stream()
                    .map(TransactionResponse::from).toList();
            PaginationInfo pagination = PaginationInfo.builder()
                    .page(pageable.getPageNumber())
                    .size(pageable.getPageSize())
                    .totalElements(transactionUseCase.countAccountTransactions(effectiveAccountId, userId))
                    .totalPages(transactions.size() < pageable.getPageSize() ? 1 :
                            (int) Math.ceil((double) transactions.size() / pageable.getPageSize()))
                    .hasNext(transactions.size() >= pageable.getPageSize())
                    .hasPrevious(pageable.getPageNumber() > 0)
                    .build();
            return ResponseEntity.ok(ApiResponse.success(responseList, pagination));
        } catch (BusinessException e) {
            log.warn("Error retrieving transactions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getCode(), e.getMessage()));
        }
    }

    /**
     * Initiate a fund transfer to another account.
     * Supports BI-FAST, SKN, and internal transfers.
     */
    @PostMapping("/transfer")
    @Audited(
            operation = id.payu.security.annotation.AuditOperation.TRANSFER,
            entityType = "TransactionEntity",
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
            @Valid @RequestBody InitiateTransferRequest request,
            @RequestHeader(value = "X-StepUp-Challenge-Id", required = false) String challengeIdHeader,
            @RequestHeader(value = "X-Transaction-PIN", required = false) String pinHeader
    ) {
        try {
            String userId = extractUserId();
            InitiateTransferCommand base = InitiateTransferCommand.from(request, userId);
            // ADR-0028: support both body and header for step-up proof (WYSIWYS)
            String effectiveChallengeId = base.stepUpChallengeId() != null && !base.stepUpChallengeId().isBlank()
                    ? base.stepUpChallengeId() : challengeIdHeader;
            String effectivePin = base.transactionPin() != null && !base.transactionPin().isBlank()
                    ? base.transactionPin() : pinHeader;
            InitiateTransferCommand command = new InitiateTransferCommand(
                    base.senderAccountId(), base.recipientAccountNumber(), base.amount(), base.description(),
                    base.type(), effectivePin, base.deviceId(), base.idempotencyKey(), base.userId(), base.bankCode(), effectiveChallengeId);
            InitiateTransferCommandResult result = transactionUseCase.initiateTransfer(command);
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
     * ADR-0028 Step-Up prepare — dynamic linking challenge per PSD2 RTS Art5.
     * Returns challengeId (TTL 180s) for WYSIWYS display + PIN entry.
     * Client must forward challengeId + PIN via headers X-StepUp-Challenge-Id + X-Transaction-PIN on POST /transfer.
     */
    @PostMapping("/transfer/prepare")
    @Operation(summary = "Prepare step-up challenge", description = "Creates dynamic linking challenge (SHA-256 sender|recipient|amount|currency) TTL 180s per ADR-0028. Returns challengeId for PIN entry; verify via X-StepUp-Challenge-Id + X-Transaction-PIN on transfer execute.")
    @PreAuthorize("hasAuthority('write:transaction')")
    public ResponseEntity<ApiResponse<PrepareChallengeResponse>> prepareTransfer(
            @Valid @RequestBody InitiateTransferRequest request) {
        String userId = extractUserId();
        String currency = request.getCurrency() != null ? request.getCurrency() : "IDR";
        String challengeId = stepUpVerificationPort.createChallenge(
                userId, request.getSenderAccountId(), request.getRecipientAccountNumber(),
                request.getAmount(), currency);
        PrepareChallengeResponse body = new PrepareChallengeResponse(challengeId, 180L,
                request.getRecipientAccountNumber(), request.getAmount().toPlainString(), currency);
        return ResponseEntity.ok(ApiResponse.success(body));
    }

    public record PrepareChallengeResponse(String challengeId, long ttlSeconds, String recipientAccountNumber, String amount, String currency) {}

    @PostMapping("/interbank/callback")
    @Operation(summary = "Settle interbank transfer", description = "Applies a signed BI-FAST, SKN, or RTGS status callback")
    public ResponseEntity<ApiResponse<TransactionResponse>> settleInterbankTransfer(
            @Valid @RequestBody InterbankTransferCallbackRequest request) {
        TransactionEntity transaction = transactionUseCase.settleInterbankTransfer(
                request.referenceNumber(), request.status(), request.failureReason());
        return ok(TransactionResponse.from(transaction));
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
                    description = "TransactionEntity found",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "TransactionEntity not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PreAuthorize("hasAuthority('read:transaction')")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @Parameter(description = "TransactionEntity ID", required = true)
            @PathVariable UUID transactionId
    ) {
        try {
            String userId = extractUserId();
            TransactionEntity transaction = transactionUseCase.getTransaction(transactionId, userId);
            // BUG-BE-135 FIX: Return DTO instead of domain entity
            return ok(TransactionResponse.from(transaction));
        } catch (BusinessException e) {
            log.warn("TransactionEntity not found: {}", transactionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getCode(), e.getMessage()));
        }
    }

    /**
     * Read-only transaction data required by the dispute service to create refunds.
     */
    @GetMapping("/internal/{transactionId}/refund-details")
    @PreAuthorize("hasAnyRole('ADMIN', 'BACKOFFICE', 'DISPUTE_AGENT')")
    public ResponseEntity<TransactionRefundDetailsResponse> getTransactionRefundDetails(
            @PathVariable UUID transactionId) {
        return ResponseEntity.ok(transactionUseCase.getTransactionRefundDetails(transactionId));
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
            @RequestParam(defaultValue = "20") @Max(value = 100, message = "Page size must not exceed 100") int size,

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
            List<TransactionEntity> transactions = transactionUseCase.getAccountTransactions(
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
                    .totalElements(transactionUseCase.countAccountTransactions(accountId, userId))
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
     * Aggregate summary of an account's transactions (GRPC-008).
     * Consumed by lending-service enhanced credit scoring.
     */
    @GetMapping("/accounts/{accountId}/summary")
    @Operation(
            summary = "Get account transaction summary",
            description = "Aggregates transaction totals, success/failure counts and date range for an account."
    )
    @PreAuthorize("hasAuthority('read:transaction')")
    public ResponseEntity<ApiResponse<TransactionSummaryResponse>> getAccountSummary(
            @Parameter(description = "Account ID", required = true)
            @PathVariable UUID accountId
    ) {
        try {
            String userId = extractUserId();
            return ResponseEntity.ok(ApiResponse.success(
                    accountTransactionSummaryService.summarize(accountId, userId)));
        } catch (BusinessException e) {
            log.warn("Error retrieving summary for account: {} - {}", accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getCode(), e.getMessage()));
        }
    }

    /**
     * Process QRIS payment.
     */
    @PostMapping("/qris/pay")
    @Audited(
            operation = id.payu.security.annotation.AuditOperation.TRANSFER,
            entityType = "TransactionEntity",
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
            @Valid @RequestBody ProcessQrisPaymentRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = true) String idempotencyKey
    ) {
        try {
            String userId = extractUserId();
            transactionUseCase.processQrisPayment(request, userId, idempotencyKey);
            return ResponseEntity.accepted()
                    .body(ApiResponse.<Void>success(null));
        } catch (BusinessException e) {
            log.warn("QRIS payment failed: {}", e.getMessage());
            return ResponseEntity.unprocessableEntity()
                    .body(ApiResponse.error(e.getCode(), e.getMessage()));
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // READY-066: External QRIS service unreachable. Mirror the bifast pattern
            // (processDisbursement catches Exception). Log + return 503 so the client
            // can retry. In production, add a circuit breaker to QrisServiceAdapter
            // (like bifast) and a Kafka event to retry asynchronously.
            log.error("QRIS external service unavailable for account={}: {}",
                    request.getAccountId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("QRIS_SERVICE_UNAVAILABLE",
                            "QRIS service temporarily unavailable. Please retry later."));
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
                    description = "TransactionEntity not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PreAuthorize("hasAuthority('write:transaction')")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransactionTags(
            @Parameter(description = "TransactionEntity ID", required = true)
            @PathVariable UUID transactionId,
            @Valid @RequestBody UpdateTransactionTagsRequest request
    ) {
        try {
            String userId = extractUserId();
            TransactionEntity transaction = transactionUseCase.updateTransactionTags(transactionId, userId, request.getTags());
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
