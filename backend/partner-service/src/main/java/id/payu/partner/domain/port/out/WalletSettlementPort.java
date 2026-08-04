package id.payu.partner.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Settles a payment between two wallet accounts.
 */
public interface WalletSettlementPort {

    void settle(String sourceAccountId, String beneficiaryAccountId,
                BigDecimal amount, String currency, String referenceId);

    void reverse(String senderAccountId, String recipientAccountId,
                 BigDecimal amount, String currency, UUID refundId, String description);
}
