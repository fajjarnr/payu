package id.payu.dispute.domain.port.out;

import id.payu.dispute.domain.model.TransactionDetails;

import java.util.Optional;
import java.util.UUID;

/**
 * Read-only port for authoritative transaction data used by refunds.
 */
public interface TransactionLookupPort {

    Optional<TransactionDetails> findById(UUID transactionId);
}
