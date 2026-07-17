package id.payu.statement.domain.model;

/** Value object representing recipient information for a transaction receipt. */
public record RecipientInfo(String name, String accountNumber, String bankName) {

    public String getName() { return name; }
    public String getAccountNumber() { return accountNumber; }
    public String getBankName() { return bankName; }

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
