package id.payu.billing.adapter.client;

import id.payu.billing.domain.port.out.WalletPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Adapter wrapping WalletClient to implement WalletPort.
 */
@Component
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
}
