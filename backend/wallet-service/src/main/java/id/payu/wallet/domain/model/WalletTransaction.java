package id.payu.wallet.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WalletTransaction represents a ledger entry for wallet operations.
 * Each transaction is immutable - records CREDIT or DEBIT operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {

    private UUID id;
    private UUID walletId;
    private String referenceId;
    private TransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private LocalDateTime createdAt;

    public enum TransactionType {
        CREDIT,
        DEBIT
    }
}
