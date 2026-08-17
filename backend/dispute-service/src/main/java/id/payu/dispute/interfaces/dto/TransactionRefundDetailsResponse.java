package id.payu.dispute.interfaces.dto;

import java.math.BigDecimal;

/**
 * Response contract for the transaction-service refund-details query.
 */
public record TransactionRefundDetailsResponse(BigDecimal amount, String currency,
                                               String senderAccountId, String recipientAccountId) {
}
