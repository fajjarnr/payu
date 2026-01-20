package id.payu.wallet.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntry {
    private UUID id;
    private UUID transactionId;
    private UUID accountId;
    private EntryType entryType;
    private BigDecimal amount;
    private String currency;
    private BigDecimal balanceAfter;
    private String referenceType;
    private String referenceId;
    private LocalDateTime createdAt;

    public enum EntryType {
        DEBIT,
        CREDIT
    }
}
