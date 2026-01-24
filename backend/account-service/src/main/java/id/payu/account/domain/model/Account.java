package id.payu.account.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Account domain model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    private UUID id;
    private String externalId;
    private UUID userId;
    private String accountNumber;
    private String accountType;
    private AccountStatus status;
    private BigDecimal balance;
    private String currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum AccountStatus {
        ACTIVE,
        FROZEN,
        CLOSED,
        PENDING_VERIFICATION
    }
}
