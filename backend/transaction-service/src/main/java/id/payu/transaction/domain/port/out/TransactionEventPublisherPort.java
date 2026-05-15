package id.payu.transaction.domain.port.out;

import id.payu.transaction.adapter.persistence.entity.TransactionEntity;

public interface TransactionEventPublisherPort {
    void publishTransactionInitiated(TransactionEntity transaction);
    void publishTransactionValidated(TransactionEntity transaction);
    void publishTransactionCompleted(TransactionEntity transaction);
    void publishTransactionFailed(TransactionEntity transaction, String reason);
}
