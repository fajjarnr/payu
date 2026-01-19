package id.payu.transaction.domain.port.out;

import id.payu.transaction.domain.model.Transaction;

public interface TransactionEventPublisherPort {
    void publishTransactionInitiated(Transaction transaction);
    void publishTransactionValidated(Transaction transaction);
    void publishTransactionCompleted(Transaction transaction);
    void publishTransactionFailed(Transaction transaction, String reason);
}
