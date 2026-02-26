package id.payu.wallet.domain.port.in;

import id.payu.wallet.domain.model.EscrowTransaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Input port for escrow / payment holding use cases.
 * <p>
 * Supports marketplace payment flows where buyer funds are held
 * until conditions are met (goods received, event completed, etc.).
 */
public interface EscrowUseCase {

    /**
     * Create and hold an escrow — reserves buyer funds immediately.
     *
     * @param buyerAccountId    buyer's account ID
     * @param sellerAccountId   seller/merchant account ID
     * @param partnerId         partner ID (TokoBapak, Nobar, etc.) — nullable for internal
     * @param amount            total escrow amount
     * @param feeAmount         platform fee amount (nullable, defaults to 0)
     * @param currency          currency code (e.g., IDR)
     * @param externalReferenceId partner's external reference (order ID, booking ID)
     * @param description       human-readable description
     * @param expiresInHours    hours until auto-expiry (default 72h)
     * @return the created escrow with HELD status
     */
    EscrowTransaction createAndHoldEscrow(String buyerAccountId, String sellerAccountId,
                                          String partnerId, BigDecimal amount,
                                          BigDecimal feeAmount, String currency,
                                          String externalReferenceId, String description,
                                          int expiresInHours);

    /**
     * Release escrow to merchant — conditions met.
     * DR Escrow Holdings (2100) / CR Merchant Payable (2200).
     *
     * @param escrowId escrow transaction ID
     * @return updated escrow in RELEASED status
     */
    EscrowTransaction releaseEscrow(UUID escrowId);

    /**
     * Settle escrow — credit merchant wallet.
     * DR Merchant Payable (2200) / CR Merchant Wallet (1100).
     *
     * @param escrowId escrow transaction ID
     * @return updated escrow in SETTLED status
     */
    EscrowTransaction settleEscrow(UUID escrowId);

    /**
     * Refund escrow back to buyer.
     * DR Escrow Holdings (2100) / CR Buyer Wallet (1100).
     *
     * @param escrowId escrow transaction ID
     * @param reason   refund reason
     * @return updated escrow in REFUNDED status
     */
    EscrowTransaction refundEscrow(UUID escrowId, String reason);

    /**
     * Get escrow by ID.
     */
    EscrowTransaction getEscrow(UUID escrowId);

    /**
     * Get escrows by buyer account.
     */
    List<EscrowTransaction> getEscrowsByBuyer(String buyerAccountId);

    /**
     * Get escrows by seller account.
     */
    List<EscrowTransaction> getEscrowsBySeller(String sellerAccountId);

    /**
     * Get escrows by partner.
     */
    List<EscrowTransaction> getEscrowsByPartner(String partnerId);

    /**
     * Process expired escrows — auto-refund.
     * Called by scheduler.
     */
    void processExpiredEscrows();
}
