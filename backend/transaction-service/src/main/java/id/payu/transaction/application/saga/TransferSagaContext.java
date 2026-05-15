package id.payu.transaction.application.saga;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Saga context for inter-bank transfer orchestration (BiFast/SKN/RGS).
 * <p>
 * Carries all the state needed across saga steps:
 * - TransactionEntity identity
 * - Sender/recipient details
 * - Reservation tracking for compensation
 * <p>
 * This context is persisted as JSONB in the saga_instances table,
 * enabling recovery after crashes.
 */
public class TransferSagaContext {
    public TransferSagaContext() {
    }

    public TransferSagaContext(UUID transactionId, String referenceNumber, UUID senderAccountId, String recipientAccountNumber, String recipientBankCode, BigDecimal amount, String currency, String transferType, String reservationId, String externalTransferReference, boolean balanceReserved, boolean externalTransferInitiated) {
        this.transactionId = transactionId;
        this.referenceNumber = referenceNumber;
        this.senderAccountId = senderAccountId;
        this.recipientAccountNumber = recipientAccountNumber;
        this.recipientBankCode = recipientBankCode;
        this.amount = amount;
        this.currency = currency;
        this.transferType = transferType;
        this.reservationId = reservationId;
        this.externalTransferReference = externalTransferReference;
        this.balanceReserved = balanceReserved;
        this.externalTransferInitiated = externalTransferInitiated;
    }

    public static TransferSagaContextBuilder builder() {
        return new TransferSagaContextBuilder();
    }

    public static class TransferSagaContextBuilder {
        private UUID transactionId;
        private String referenceNumber;
        private UUID senderAccountId;
        private String recipientAccountNumber;
        private String recipientBankCode;
        private BigDecimal amount;
        private String currency;
        private String transferType;
        private String reservationId;
        private String externalTransferReference;
        private boolean balanceReserved;
        private boolean externalTransferInitiated;

        public TransferSagaContextBuilder transactionId(UUID transactionId) {
            this.transactionId = transactionId;
            return this;
        }
        public TransferSagaContextBuilder referenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
            return this;
        }
        public TransferSagaContextBuilder senderAccountId(UUID senderAccountId) {
            this.senderAccountId = senderAccountId;
            return this;
        }
        public TransferSagaContextBuilder recipientAccountNumber(String recipientAccountNumber) {
            this.recipientAccountNumber = recipientAccountNumber;
            return this;
        }
        public TransferSagaContextBuilder recipientBankCode(String recipientBankCode) {
            this.recipientBankCode = recipientBankCode;
            return this;
        }
        public TransferSagaContextBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }
        public TransferSagaContextBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }
        public TransferSagaContextBuilder transferType(String transferType) {
            this.transferType = transferType;
            return this;
        }
        public TransferSagaContextBuilder reservationId(String reservationId) {
            this.reservationId = reservationId;
            return this;
        }
        public TransferSagaContextBuilder externalTransferReference(String externalTransferReference) {
            this.externalTransferReference = externalTransferReference;
            return this;
        }
        public TransferSagaContextBuilder balanceReserved(boolean balanceReserved) {
            this.balanceReserved = balanceReserved;
            return this;
        }
        public TransferSagaContextBuilder externalTransferInitiated(boolean externalTransferInitiated) {
            this.externalTransferInitiated = externalTransferInitiated;
            return this;
        }

        public TransferSagaContext build() {
            return new TransferSagaContext(transactionId, referenceNumber, senderAccountId, recipientAccountNumber, recipientBankCode, amount, currency, transferType, reservationId, externalTransferReference, balanceReserved, externalTransferInitiated);
        }
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public UUID getSenderAccountId() {
        return senderAccountId;
    }

    public void setSenderAccountId(UUID senderAccountId) {
        this.senderAccountId = senderAccountId;
    }

    public String getRecipientAccountNumber() {
        return recipientAccountNumber;
    }

    public void setRecipientAccountNumber(String recipientAccountNumber) {
        this.recipientAccountNumber = recipientAccountNumber;
    }

    public String getRecipientBankCode() {
        return recipientBankCode;
    }

    public void setRecipientBankCode(String recipientBankCode) {
        this.recipientBankCode = recipientBankCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getTransferType() {
        return transferType;
    }

    public void setTransferType(String transferType) {
        this.transferType = transferType;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getExternalTransferReference() {
        return externalTransferReference;
    }

    public void setExternalTransferReference(String externalTransferReference) {
        this.externalTransferReference = externalTransferReference;
    }

    public boolean isBalanceReserved() {
        return balanceReserved;
    }

    public void setBalanceReserved(boolean balanceReserved) {
        this.balanceReserved = balanceReserved;
    }

    public boolean isExternalTransferInitiated() {
        return externalTransferInitiated;
    }

    public void setExternalTransferInitiated(boolean externalTransferInitiated) {
        this.externalTransferInitiated = externalTransferInitiated;
    }



    /** The transaction ID being processed */
    private UUID transactionId;

    /** Reference number for idempotency */
    private String referenceNumber;

    /** Sender's account ID for wallet operations */
    private UUID senderAccountId;

    /** Recipient's account number (external bank) */
    private String recipientAccountNumber;

    /** Recipient's bank code */
    private String recipientBankCode;

    /** Transfer amount in IDR */
    private BigDecimal amount;

    /** Currency code */
    private String currency;

    /** Transfer channel: BIFAST, SKN, RGS */
    private String transferType;

    // --- Intermediate state for compensation ---

    /** Wallet reservation ID (set after RESERVE_BALANCE step) */
    private String reservationId;

    /** External transfer reference from BiFast/SKN/RGS (set after INITIATE_TRANSFER step) */
    private String externalTransferReference;

    /** Flag indicating if balance was reserved (for compensation) */
    private boolean balanceReserved;

    /** Flag indicating if external transfer was initiated */
    private boolean externalTransferInitiated;
}
