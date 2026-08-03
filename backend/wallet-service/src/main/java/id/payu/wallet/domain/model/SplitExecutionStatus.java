package id.payu.wallet.domain.model;

public enum SplitExecutionStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        RECONCILIATION_REQUIRED,
        REVERSED
    }
