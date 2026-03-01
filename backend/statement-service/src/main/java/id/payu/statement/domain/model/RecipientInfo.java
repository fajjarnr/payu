package id.payu.statement.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Value Object representing recipient information for a transaction receipt.
 * Epic E-19: Transaction Proof & Receipts (IMP-055)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RecipientInfo {

    private String name;
    private String accountNumber;
    private String bankName;

    /**
     * Validates that all required fields are present.
     *
     * @throws IllegalArgumentException if any required field is invalid
     */
    public void validate() {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Recipient name is required");
        }
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Recipient account number is required");
        }
        if (bankName == null || bankName.isBlank()) {
            throw new IllegalArgumentException("Recipient bank name is required");
        }
    }
}
