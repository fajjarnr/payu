package id.payu.dispute.domain.port.in;

import id.payu.dispute.domain.model.Refund;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port for refund use cases.
 *
 * <p>Defines the operations that can be performed on refunds.
 * This interface is implemented by application services.</p>
 */
public interface RefundUseCase {

    Refund createFullRefund(UUID transactionId, String reason);

    Refund createPartialRefund(UUID transactionId, BigDecimal amount, String currency, String reason);

    Refund processRefund(UUID refundId);

    Refund completeRefund(UUID refundId);

    Refund failRefund(UUID refundId, String failureReason);

    Refund cancelRefund(UUID refundId, String cancellationReason);

    Optional<Refund> getRefund(UUID refundId);

    List<Refund> getRefundsByTransaction(UUID transactionId);

    List<Refund> getRefundsByStatus(String status);
}
