package id.payu.wallet.domain.port.out;

import java.math.BigDecimal;

/**
 * Output port for publishing wallet events to message broker.
 */
public interface WalletEventPublisherPort {
    
    void publishWalletCreated(String accountId, String walletId);
    
    void publishBalanceChanged(String accountId, BigDecimal newBalance, BigDecimal availableBalance);
    
    void publishBalanceReserved(String accountId, String reservationId, BigDecimal amount);
    
    void publishReservationCommitted(String accountId, String reservationId, BigDecimal amount);
    
    void publishReservationReleased(String accountId, String reservationId, BigDecimal amount);
}
