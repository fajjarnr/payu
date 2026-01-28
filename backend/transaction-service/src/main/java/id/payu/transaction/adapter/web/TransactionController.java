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
import id.payu.transaction.application.cqrs.command.InitiateTransferCommand;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommandResult;
import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.in.TransactionUseCase;
import id.payu.transaction.dto.InitiateTransferRequest;
import id.payu.transaction.dto.InitiateTransferResponse;
import id.payu.transaction.dto.ProcessQrisPaymentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@Slf4j
@Tag(name = OpenApiConstants.Tags.TRANSACTIONS, description = "Fund transfer and payment endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController extends BaseController {

    private final TransactionUseCase transactionUseCase;

    /**
     * Extracts the user ID from the JWT authentication token.
     *
     * @return The user ID from the JWT subject claim
     * @throws IllegalStateException if no authentication is present
     */
    private String extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            throw new IllegalStateException("No valid JWT authentication found");
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        return jwt.getSubject(); // In production, use a specific claim like "user_id" or "sub"
    }

    /**
     * Initiate a fund transfer to another account.
     * Supports BI-FAST, SKN, and internal transfers.
     */
    @PostMapping("/transfer")
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
    @ApiResponses(value = {
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
        } catch (Exception e) {
            log.error("Unexpected error during transfer initiation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            ErrorCode.INTERNAL_ERROR.getCode(),
                            ErrorCode.INTERNAL_ERROR.getMessage()
                    ));
        }
    }

    /**
     * Get transaction details by transaction ID.
     */
    @GetMapping("/{transactionId}")
    @Operation(
            summary = "Get transaction details",
            description = "Retrieves detailed information about a specific transaction."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Transaction found",
                    content = @Content(schema = @Schema(implementation = Transaction.class))
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
    public ResponseEntity<ApiResponse<Transaction>> getTransaction(
            @Parameter(description = "Transaction ID", required = true)
            @PathVariable UUID transactionId
    ) {
        try {
            String userId = extractUserId();
            Transaction transaction = transactionUseCase.getTransaction(transactionId, userId);
            return ok(transaction);
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
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Transactions retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Transaction.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            )
    })
    @PreAuthorize("hasAuthority('read:transaction')")
    public ResponseEntity<ApiResponse<List<Transaction>>> getAccountTransactions(
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

            // Create pageable from parameters
            var pageable = createPageable(page, size, sort, ApiConstants.DEFAULT_SORT_DIRECTION);

            // Get transactions (Note: UseCase might need update for Page return)
            List<Transaction> transactions = transactionUseCase.getAccountTransactions(
                    accountId,
                    userId,
                    pageable.getPageNumber(),
                    pageable.getPageSize()
            );

            // TODO: Update UseCase to return Page for proper pagination
            return ok(transactions);
        } catch (Exception e) {
            log.error("Error retrieving transactions for account: {}", accountId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            ErrorCode.INTERNAL_ERROR.getCode(),
                            ErrorCode.INTERNAL_ERROR.getMessage()
                    ));
        }
    }

    /**
     * Process QRIS payment.
     */
    @PostMapping("/qris/pay")
    @Operation(
            summary = "Process QRIS payment",
            description = """
                    Processes a payment through QRIS (Quick Response Code Indonesian Standard).

                    **Rate Limiting:** 100 requests per minute

                    **Idempotency:** Use `Idempotency-Key` header to prevent duplicate payments
                    """
    )
    @ApiResponses(value = {
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
        } catch (Exception e) {
            log.error("Unexpected error during QRIS payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            ErrorCode.INTERNAL_ERROR.getCode(),
                            ErrorCode.INTERNAL_ERROR.getMessage()
                    ));
        }
    }

    @Override
    protected String getBaseUrl() {
        return "/api/v1/transactions";
    }
}
