package id.payu.partner.domain.port.out;

import java.math.BigDecimal;

/**
 * Settles a payment between two wallet accounts.
 */
public interface WalletSettlementPort {

    void settle(String sourceAccountId, String beneficiaryAccountId,
                BigDecimal amount, String currency, String referenceId);
}
