package id.payu.security.annotation;

public enum SensitivityLevel {
        /**
         * Standard masking: partial visibility (e.g., email, phone).
         */
        STANDARD,

        /**
         * High masking: only last 4 digits visible (e.g., account numbers).
         */
        HIGH,

        /**
         * Critical masking: fully masked (e.g., passwords, PINs, tokens).
         */
        CRITICAL
    }
