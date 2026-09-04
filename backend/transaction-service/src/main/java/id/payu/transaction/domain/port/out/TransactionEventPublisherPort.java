package id.payu.transaction.domain.port.out;

import id.payu.transaction.adapter.persistence.entity.TransactionEntity;

public interface TransactionEventPublisherPort {
    void publishTransactionInitiated(TransactionEntity transaction, String userId);
    void publishTransactionValidated(TransactionEntity transaction);
    void publishTransactionCompleted(TransactionEntity transaction, String userId);
    void publishTransactionFailed(TransactionEntity transaction, String reason);
}
