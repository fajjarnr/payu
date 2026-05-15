package id.payu.partner.domain;

public enum Status {
        PENDING,
        DELIVERING,
        DELIVERED,
        FAILED,
        EXHAUSTED  // max retries exceeded
    }
