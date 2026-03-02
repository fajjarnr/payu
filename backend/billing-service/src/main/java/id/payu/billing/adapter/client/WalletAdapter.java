package id.payu.billing.adapter.client;

import id.payu.billing.domain.port.out.WalletPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * REST adapter wrapping WalletClient to implement WalletPort.
 *
 * @deprecated Use {@link WalletGrpcAdapter} instead (IMP-028: gRPC migration).
 *             Kept as fallback during migration period.
 */
@Deprecated
@Component("walletRestAdapter")
@RequiredArgsConstructor
public class WalletAdapter implements WalletPort {

    private final WalletClient walletClient;

    @Override
    public ReserveResult reserveBalance(String accountId, BigDecimal amount, String referenceNumber) {
        WalletClient.ReserveResponse response = walletClient.reserveBalance(
                accountId,
                new WalletClient.ReserveRequest(amount, referenceNumber)
        );
        return new ReserveResult(response.reservationId(), response.status());
    }

    @Override
    public void commitReservation(String reservationId) {
        walletClient.commitReservation(reservationId);
    }

    @Override
    public void releaseReservation(String reservationId) {
        walletClient.releaseReservation(reservationId);
    }
}
