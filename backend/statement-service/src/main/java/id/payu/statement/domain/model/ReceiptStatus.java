package id.payu.statement.domain.model;

/**
 * Status enum for Receipt lifecycle.
 * Epic E-19: Transaction Proof & Receipts (IMP-055)
 */
public enum ReceiptStatus {
    GENERATED,   // Receipt has been generated and is available
    EXPIRED      // Receipt has expired (after 90 days)
}
