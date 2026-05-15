package id.payu.partner.domain;

public enum KeyStatus {
        ACTIVE,
        ROTATED,    // grace period — old key still valid temporarily
        REVOKED,
        EXPIRED
    }
