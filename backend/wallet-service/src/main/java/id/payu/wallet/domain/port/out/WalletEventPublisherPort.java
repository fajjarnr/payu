package id.payu.wallet.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Output port for publishing wallet events to message broker.
 */
public interface WalletEventPublisherPort {
    
    void publishWalletCreated(String accountId, String walletId);
    
    void publishBalanceChanged(String accountId, BigDecimal newBalance, BigDecimal availableBalance);
    
    void publishBalanceReserved(String accountId, String reservationId, BigDecimal amount);
    
    void publishReservationCommitted(String accountId, String reservationId, BigDecimal amount);
    
    void publishReservationReleased(String accountId, String reservationId, BigDecimal amount);

    // --- Escrow lifecycle events ---

    void publishEscrowHeld(UUID escrowId, String buyerAccountId, String sellerAccountId,
                           String partnerId, BigDecimal amount, String currency,
                           String externalReferenceId);

    void publishEscrowReleased(UUID escrowId, String partnerId, BigDecimal amount, String currency);

    void publishEscrowSettled(UUID escrowId, String sellerAccountId, String partnerId,
                              BigDecimal netAmount, String currency);

    void publishEscrowRefunded(UUID escrowId, String buyerAccountId, String partnerId,
                               BigDecimal amount, String currency, String reason);

    void publishEscrowExpired(UUID escrowId, String partnerId, BigDecimal amount, String currency);
}
