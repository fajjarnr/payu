package id.payu.transaction.domain.model;

public enum TransactionStatus {
        PENDING,
        VALIDATING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
