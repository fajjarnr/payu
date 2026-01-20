package id.payu.wallet.adapter.web;

import id.payu.wallet.application.exception.WalletNotFoundException;
import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.model.WalletTransaction;
import id.payu.wallet.domain.port.in.WalletUseCase;
import id.payu.wallet.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import id.payu.wallet.domain.model.LedgerEntry;

/**
 * REST Controller for wallet operations.
 * Driving adapter in Hexagonal Architecture.
 */
@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WalletController.class);

    private final WalletUseCase walletUseCase;

    public WalletController(WalletUseCase walletUseCase) {
        this.walletUseCase = walletUseCase;
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String accountId) {
        log.info("Getting balance for account: {}", accountId);

        Wallet wallet = walletUseCase.getWalletByAccountId(accountId)
                .orElseThrow(() -> new WalletNotFoundException(accountId));

        BalanceResponse response = BalanceResponse.builder()
                .accountId(accountId)
                .balance(wallet.getBalance())
                .availableBalance(wallet.getAvailableBalance())
                .reservedBalance(wallet.getReservedBalance())
                .currency(wallet.getCurrency())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountId}/reserve")
    public ResponseEntity<ReserveBalanceResponse> reserveBalance(
            @PathVariable String accountId,
            @Valid @RequestBody ReserveBalanceRequest request) {
        log.info("Reserving {} for account: {}", request.getAmount(), accountId);

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

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reservations/{reservationId}/commit")
    public ResponseEntity<Map<String, String>> commitReservation(@PathVariable String reservationId) {
        log.info("Committing reservation: {}", reservationId);
        walletUseCase.commitReservation(reservationId);
        return ResponseEntity.ok(Map.of("status", "COMMITTED", "reservationId", reservationId));
    }

    @PostMapping("/reservations/{reservationId}/release")
    public ResponseEntity<Map<String, String>> releaseReservation(@PathVariable String reservationId) {
        log.info("Releasing reservation: {}", reservationId);
        walletUseCase.releaseReservation(reservationId);
        return ResponseEntity.ok(Map.of("status", "RELEASED", "reservationId", reservationId));
    }

    @PostMapping("/{accountId}/credit")
    public ResponseEntity<Map<String, String>> credit(
            @PathVariable String accountId,
            @Valid @RequestBody CreditRequest request) {
        log.info("Crediting {} to account: {}", request.getAmount(), accountId);

        walletUseCase.credit(
                accountId,
                request.getAmount(),
                request.getReferenceId(),
                request.getDescription()
        );

        return ResponseEntity.ok(Map.of("status", "CREDITED", "accountId", accountId));
    }

    @GetMapping("/{accountId}/ledger")
    public ResponseEntity<List<LedgerEntry>> getLedgerEntries(@PathVariable String accountId) {
        log.info("Getting ledger entries for account: {}", accountId);
        List<LedgerEntry> ledgerEntries = walletUseCase.getLedgerEntriesByAccountId(UUID.fromString(accountId));
        return ResponseEntity.ok(ledgerEntries);
    }

    @GetMapping("/{accountId}/ledger/transaction/{transactionId}")
    public ResponseEntity<List<LedgerEntry>> getLedgerEntriesByTransaction(
            @PathVariable String accountId,
            @PathVariable String transactionId) {
        log.info("Getting ledger entries for transaction: {}", transactionId);
        List<LedgerEntry> ledgerEntries = walletUseCase.getLedgerEntriesByTransactionId(UUID.fromString(transactionId));
        return ResponseEntity.ok(ledgerEntries);
    }

    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<List<WalletTransaction>> getTransactionHistory(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Getting transaction history for account: {}", accountId);
        List<WalletTransaction> transactions = walletUseCase.getTransactionHistory(accountId, page, size);
        return ResponseEntity.ok(transactions);
    }
}
