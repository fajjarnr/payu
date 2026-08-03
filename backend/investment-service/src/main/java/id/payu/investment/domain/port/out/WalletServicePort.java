package id.payu.investment.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Output port for wallet service operations.
 * Used to debit/credit user wallets for investment operations.
 */
public interface WalletServicePort {

    /**
     * Deduct balance from user wallet for investment purchase.
     *
     * @param userId the user ID
     * @param amount the amount to deduct
     * @throws CompletionException if deduction fails
     */
    default void deductBalance(String userId, BigDecimal amount) {
        deductBalance(userId, amount, UUID.randomUUID().toString());
    }

    void deductBalance(String userId, BigDecimal amount, String referenceId);

    /**
     * Credit balance to user wallet from investment redemption/profit.
     *
     * @param userId the user ID
     * @param amount the amount to credit
     * @throws CompletionException if credit fails
     */
    default void creditBalance(String userId, BigDecimal amount) {
        creditBalance(userId, amount, UUID.randomUUID().toString());
    }

    void creditBalance(String userId, BigDecimal amount, String referenceId);

    /**
     * Check if user has sufficient balance for investment.
     *
     * @param userId the user ID
     * @param amount the amount to check
     * @return true if user has sufficient balance, false otherwise
     */
    boolean hasSufficientBalance(String userId, BigDecimal amount);
}
