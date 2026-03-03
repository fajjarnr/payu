package id.payu.wallet.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.commons.idempotency.Idempotent;
import id.payu.wallet.application.exception.WalletNotFoundException;
import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.model.WalletTransaction;
import id.payu.wallet.domain.port.in.WalletUseCase;
import id.payu.wallet.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import id.payu.security.annotation.Audited;
import id.payu.security.annotation.Audited.AuditLevel;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for wallet operations.
 * Driving adapter in Hexagonal Architecture.
 */
@RestController
@RequestMapping("/api/v1/wallets")
@Tag(name = "Wallet", description = "Wallet management APIs for balance, reservations, and ledger operations")
@SecurityRequirement(name = "bearerAuth")
public class WalletController extends BaseController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WalletController.class);

    private final WalletUseCase walletUseCase;

    public WalletController(WalletUseCase walletUseCase) {
        this.walletUseCase = walletUseCase;
    }

    /**
     * Masks an account ID for safe logging.
     * e.g., "abcd-1234-efgh" → "abcd****"
     */
    private String maskId(String id) {
        if (id == null || id.length() <= 4) return "****";
        return id.substring(0, 4) + "****";
    }

    @GetMapping("/{accountId}/balance")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get wallet balance", description = "Retrieve current balance, available balance, and reserved balance for an account")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Balance retrieved successfully",
            content = @Content(schema = @Schema(implementation = BalanceResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Wallet not found for the given account")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - valid JWT token required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - account access denied")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(
            @Parameter(description = "Account ID", required = true) @PathVariable String accountId) {
        log.info("Getting balance for account: {}", maskId(accountId));

        Wallet wallet = walletUseCase.getWalletByAccountId(accountId)
                .orElseThrow(() -> new WalletNotFoundException(accountId));

        BalanceResponse response = BalanceResponse.builder()
                .accountId(accountId)
                .balance(wallet.getBalance())
                .availableBalance(wallet.getAvailableBalance())
                .reservedBalance(wallet.getReservedBalance())
                .currency(wallet.getCurrency())
                .build();

        return ok(response);
    }

    @PostMapping("/{accountId}/reserve")
    @Idempotent(required = true)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Reserve balance", description = "Reserve a specific amount from wallet balance for pending transactions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Balance reserved successfully",
            content = @Content(schema = @Schema(implementation = ReserveBalanceResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request - insufficient balance or validation error")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Wallet not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - account access denied")
    public ResponseEntity<ApiResponse<ReserveBalanceResponse>> reserveBalance(
            @Parameter(description = "Account ID", required = true) @PathVariable String accountId,
            @Valid @RequestBody ReserveBalanceRequest request) {
        log.info("Reserving {} for account: {}", request.getAmount(), maskId(accountId));

        String reservationId = walletUseCase.reserveBalance(
                accountId,
                request.getAmount(),
                request.getReferenceId()
        );

        ReserveBalanceResponse response = ReserveBalanceResponse.builder()
                .reservationId(reservationId)
                .accountId(accountId)
                .referenceId(request.getReferenceId())
                .status("RESERVED")
                .build();

        return ok(response);
    }

    @PostMapping("/reservations/{reservationId}/commit")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Commit reservation", description = "Commit a reserved balance to complete the transaction")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reservation committed successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Reservation not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid reservation state")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - reservation access denied")
    public ResponseEntity<ApiResponse<Map<String, String>>> commitReservation(
            @Parameter(description = "Reservation ID", required = true) @PathVariable String reservationId) {
        log.info("Committing reservation: {}", reservationId);
        walletUseCase.commitReservation(reservationId);
        return ok(Map.of("status", "COMMITTED", "reservationId", reservationId));
    }

    @PostMapping("/reservations/{reservationId}/release")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Release reservation", description = "Release a reserved balance back to available balance")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reservation released successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Reservation not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid reservation state")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - reservation access denied")
    public ResponseEntity<ApiResponse<Map<String, String>>> releaseReservation(
            @Parameter(description = "Reservation ID", required = true) @PathVariable String reservationId) {
        log.info("Releasing reservation: {}", reservationId);
        walletUseCase.releaseReservation(reservationId);
        return ok(Map.of("status", "RELEASED", "reservationId", reservationId));
    }

    /**
     * Validates that the authenticated user owns the reservation.
     * Used by @PreAuthorize for ownership-based access control.
     *
     * @param reservationId the reservation ID to check
     * @param accountId the authenticated user's account ID
     * @return true if the user owns the reservation, false otherwise
     */
    public boolean validateReservationOwnership(String reservationId, String accountId) {
        try {
            String reservationOwnerId = walletUseCase.getAccountIdByReservationId(reservationId);
            return reservationOwnerId.equals(accountId);
        } catch (Exception e) {
            return false;
        }
    }

    @PostMapping("/{accountId}/credit")
    @Audited(
            operation = id.payu.security.annotation.Audited.Operation.OTHER,
            entityType = "Wallet",
            maskData = true,
            level = AuditLevel.INFO
    )
    @Idempotent(required = true)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Credit wallet", description = "Credit amount to wallet balance")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Amount credited successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Wallet not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - account access denied")
    public ResponseEntity<ApiResponse<Map<String, String>>> credit(
            @Parameter(description = "Account ID", required = true) @PathVariable String accountId,
            @Valid @RequestBody CreditRequest request) {
        log.info("Crediting {} to account: {}", request.getAmount(), maskId(accountId));

        walletUseCase.credit(
                accountId,
                request.getAmount(),
                request.getReferenceId(),
                request.getDescription()
        );

        return ok(Map.of("status", "CREDITED", "accountId", accountId));
    }

    @GetMapping("/{accountId}/ledger")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get ledger entries", description = "Retrieve all ledger entries for an account")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ledger entries retrieved successfully",
            content = @Content(schema = @Schema(implementation = LedgerEntry.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - account access denied")
    public ResponseEntity<ApiResponse<List<LedgerEntry>>> getLedgerEntries(
            @Parameter(description = "Account ID", required = true) @PathVariable String accountId) {
        log.info("Getting ledger entries for account: {}", maskId(accountId));
        List<LedgerEntry> ledgerEntries = walletUseCase.getLedgerEntriesByAccountId(accountId);
        return ok(ledgerEntries);
    }

    @GetMapping("/{accountId}/ledger/transaction/{transactionId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get ledger entries by transaction", description = "Retrieve ledger entries for a specific transaction")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ledger entries retrieved successfully",
            content = @Content(schema = @Schema(implementation = LedgerEntry.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - account access denied")
    public ResponseEntity<ApiResponse<List<LedgerEntry>>> getLedgerEntriesByTransaction(
            @Parameter(description = "Account ID", required = true) @PathVariable String accountId,
            @Parameter(description = "Transaction ID", required = true) @PathVariable String transactionId) {
        log.info("Getting ledger entries for transaction: {}", transactionId);
        try {
            List<LedgerEntry> ledgerEntries = walletUseCase.getLedgerEntriesByTransactionId(UUID.fromString(transactionId));
            return ok(ledgerEntries);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("WALLET_001", "Invalid transaction ID format"));
        }
    }

    @GetMapping("/{accountId}/transactions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get transaction history", description = "Retrieve paginated transaction history for an account")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction history retrieved successfully",
            content = @Content(schema = @Schema(implementation = WalletTransaction.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - account access denied")
    public ResponseEntity<ApiResponse<List<WalletTransaction>>> getTransactionHistory(
            @Parameter(description = "Account ID", required = true) @PathVariable String accountId,
            @Parameter(description = "Page number (default: 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20)") @RequestParam(defaultValue = "20") int size) {
        log.info("Getting transaction history for account: {}", maskId(accountId));
        List<WalletTransaction> transactions = walletUseCase.getTransactionHistory(accountId, page, size);
        return ok(transactions);
    }
}
