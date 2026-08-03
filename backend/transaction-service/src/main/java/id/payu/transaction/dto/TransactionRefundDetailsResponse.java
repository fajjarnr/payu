package id.payu.transaction.dto;

import java.math.BigDecimal;

/**
 * Minimal transaction representation exposed to the dispute service for refund creation.
 */
public record TransactionRefundDetailsResponse(BigDecimal amount, String currency,
                                               String senderAccountId, String recipientAccountId) {
}
