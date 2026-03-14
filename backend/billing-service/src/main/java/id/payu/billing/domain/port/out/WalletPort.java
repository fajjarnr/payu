package id.payu.billing.domain.port.out;

import java.math.BigDecimal;

public interface WalletPort {
    ReserveResult reserveBalance(String accountId, BigDecimal amount, String referenceNumber);
    void commitReservation(String reservationId);
    void releaseReservation(String reservationId);

    record ReserveResult(
        String reservationId,
        String status
    ) {}
}
