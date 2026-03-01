package id.payu.statement.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Value Object representing sender information for a transaction receipt.
 * Epic E-19: Transaction Proof & Receipts (IMP-055)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SenderInfo {

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
            throw new IllegalArgumentException("Sender name is required");
        }
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Sender account number is required");
        }
        if (bankName == null || bankName.isBlank()) {
            throw new IllegalArgumentException("Sender bank name is required");
        }
    }
}
