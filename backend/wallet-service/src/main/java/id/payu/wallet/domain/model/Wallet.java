package id.payu.wallet.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Wallet domain entity representing an account's balance.
 * Core domain model - no JPA annotations here (Clean Architecture).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {
    
    private UUID id;
    private String accountId;
    private BigDecimal balance;
    private BigDecimal reservedBalance;
    private String currency;
    private WalletStatus status;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Get available balance (total balance minus reserved).
     */
    public BigDecimal getAvailableBalance() {
        return balance.subtract(reservedBalance);
    }

    /**
     * Check if wallet has sufficient available balance.
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return getAvailableBalance().compareTo(amount) >= 0;
    }

    /**
     * Reserve an amount from available balance.
     */
    public void reserve(BigDecimal amount) {
        if (!hasSufficientBalance(amount)) {
            throw new IllegalStateException("Insufficient available balance");
        }
        this.reservedBalance = this.reservedBalance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Commit a reserved amount (deduct from balance).
     */
    public void commitReservation(BigDecimal amount) {
        if (reservedBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Reserved amount not found");
        }
        this.balance = this.balance.subtract(amount);
        this.reservedBalance = this.reservedBalance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Release a reserved amount back to available.
     */
    public void releaseReservation(BigDecimal amount) {
        if (reservedBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Reserved amount not found");
        }
        this.reservedBalance = this.reservedBalance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Credit amount to wallet (e.g., incoming transfer).
     */
    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    public enum WalletStatus {
        ACTIVE,
        FROZEN,
        CLOSED
    }
}
