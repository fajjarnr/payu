package id.payu.transaction.domain.port.out;

import id.payu.transaction.interfaces.dto.ReserveBalanceRequest;
import id.payu.transaction.interfaces.dto.ReserveBalanceResponse;

import java.util.UUID;

public interface WalletServicePort {
    ReserveBalanceResponse reserveBalance(UUID accountId, String transactionId, java.math.BigDecimal amount);
    void commitBalance(UUID accountId, String transactionId, String reservationId, java.math.BigDecimal amount);
    void releaseBalance(UUID accountId, String transactionId, String reservationId, java.math.BigDecimal amount);
    void creditBalance(String accountId, String transactionId, java.math.BigDecimal amount);

    /**
     * IMP-1: atomic one-hop transfer — wallet debits sender and credits recipient
     * in a single transaction (idempotent by referenceId). Replaces the
     * reserve→commit→credit sequence whose crash window needed saga compensation.
     *
     * @return the wallet ledger transaction id
     */
    String transferBalance(String senderAccountId, String recipientAccountId,
                           java.math.BigDecimal amount, String referenceId);
}
