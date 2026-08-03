package id.payu.dispute.domain.model;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Authoritative transaction values needed to create a refund.
 */
public record TransactionDetails(BigDecimal amount, String currency) {

    public TransactionDetails {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Transaction currency is required");
        }
        currency = currency.trim().toUpperCase(Locale.ROOT);
    }
}
