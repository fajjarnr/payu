package id.payu.security.annotation;

public enum AuditOperation {
        CREATE,
        READ,
        UPDATE,
        DELETE,
        TRANSFER,
        LOGIN,
        LOGOUT,
        KYC_APPROVE,
        KYC_REJECT,
        PASSWORD_CHANGE,
        PASSWORD_RESET,
        ACCOUNT_BLOCK,
        ACCOUNT_UNBLOCK,
        OTHER
    }
