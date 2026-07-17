package id.payu.statement.domain.model;

/** Value object representing sender information for a transaction receipt. */
public record SenderInfo(String name, String accountNumber, String bankName) {

    public String getName() { return name; }
    public String getAccountNumber() { return accountNumber; }
    public String getBankName() { return bankName; }

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
