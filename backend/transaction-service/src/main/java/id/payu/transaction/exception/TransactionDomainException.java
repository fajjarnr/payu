package id.payu.transaction.exception;

/**
 * Base exception for Transaction Service domain errors.
 * 
 * Error Code Structure: TXN_[CATEGORY]_[SPECIFIC]
 * 
 * Categories:
 * - VAL: Validation errors
 * - BUS: Business rule violations
 * - BAL: Balance/wallet errors
 * - EXT: External service errors (BI-FAST, QRIS)
 * - SYS: System/technical errors
 */
public abstract class TransactionDomainException extends RuntimeException {

    private final String errorCode;
    private final String userMessage;

    protected TransactionDomainException(String errorCode, String message, String userMessage) {
        super(message);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    protected TransactionDomainException(String errorCode, String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getUserMessage() {
        return userMessage;
    }

    // === Validation Errors (6000-6099) ===
    
    public static class InvalidAmountException extends TransactionDomainException {
        public InvalidAmountException(String reason) {
            super("TXN_VAL_001", 
                  "Invalid transaction amount: " + reason,
                  "Jumlah transaksi tidak valid");
        }
    }

    public static class InvalidRecipientException extends TransactionDomainException {
        public InvalidRecipientException(String recipient) {
            super("TXN_VAL_002", 
                  "Invalid recipient: " + recipient,
                  "Penerima tidak valid");
        }
    }

    public static class InvalidBankCodeException extends TransactionDomainException {
        public InvalidBankCodeException(String bankCode) {
            super("TXN_VAL_003", 
                  "Invalid bank code: " + bankCode,
                  "Kode bank tidak valid");
        }
    }

    // === Business Rule Errors (6100-6199) ===
    
    public static class DuplicateTransactionException extends TransactionDomainException {
        public DuplicateTransactionException(String referenceId) {
            super("TXN_BUS_001", 
                  "Duplicate transaction detected: " + referenceId,
                  "Transaksi duplikat terdeteksi");
        }
    }

    public static class TransactionLimitExceededException extends TransactionDomainException {
        public TransactionLimitExceededException(String limitType) {
            super("TXN_BUS_002", 
                  "Transaction limit exceeded: " + limitType,
                  "Batas transaksi terlampaui");
        }
    }

    public static class TransactionNotAllowedException extends TransactionDomainException {
        public TransactionNotAllowedException(String reason) {
            super("TXN_BUS_003", 
                  "Transaction not allowed: " + reason,
                  "Transaksi tidak diizinkan");
        }
    }

    public static class TransferToSelfException extends TransactionDomainException {
        public TransferToSelfException() {
            super("TXN_BUS_004", 
                  "Cannot transfer to own account",
                  "Tidak dapat transfer ke rekening sendiri");
        }
    }

    // === Balance/Wallet Errors (6200-6299) ===
    
    public static class InsufficientBalanceException extends TransactionDomainException {
        public InsufficientBalanceException(String accountId) {
            super("TXN_BAL_001", 
                  "Insufficient balance for account: " + accountId,
                  "Saldo tidak mencukupi");
        }
    }

    public static class BalanceReservationFailedException extends TransactionDomainException {
        public BalanceReservationFailedException(String reason) {
            super("TXN_BAL_002", 
                  "Failed to reserve balance: " + reason,
                  "Gagal mereservasi saldo");
        }

        public BalanceReservationFailedException(String reason, Throwable cause) {
            super("TXN_BAL_002", 
                  "Failed to reserve balance: " + reason,
                  "Gagal mereservasi saldo",
                  cause);
        }
    }

    public static class WalletServiceUnavailableException extends TransactionDomainException {
        public WalletServiceUnavailableException() {
            super("TXN_BAL_003", 
                  "Wallet service is unavailable",
                  "Layanan wallet sedang tidak tersedia, silakan coba lagi");
        }

        public WalletServiceUnavailableException(Throwable cause) {
            super("TXN_BAL_003", 
                  "Wallet service is unavailable",
                  "Layanan wallet sedang tidak tersedia, silakan coba lagi",
                  cause);
        }
    }

    // === External Service Errors (6300-6399) ===
    
    public static class BiFastTransferFailedException extends TransactionDomainException {
        public BiFastTransferFailedException(String reason) {
            super("TXN_EXT_001", 
                  "BI-FAST transfer failed: " + reason,
                  "Transfer BI-FAST gagal");
        }

        public BiFastTransferFailedException(String reason, Throwable cause) {
            super("TXN_EXT_001", 
                  "BI-FAST transfer failed: " + reason,
                  "Transfer BI-FAST gagal",
                  cause);
        }
    }

    public static class BiFastAccountInquiryFailedException extends TransactionDomainException {
        public BiFastAccountInquiryFailedException(String reason) {
            super("TXN_EXT_002", 
                  "BI-FAST account inquiry failed: " + reason,
                  "Gagal mengecek rekening tujuan");
        }
    }

    public static class BiFastServiceUnavailableException extends TransactionDomainException {
        public BiFastServiceUnavailableException() {
            super("TXN_EXT_003", 
                  "BI-FAST service is unavailable",
                  "Layanan BI-FAST sedang tidak tersedia, silakan coba lagi");
        }

        public BiFastServiceUnavailableException(Throwable cause) {
            super("TXN_EXT_003", 
                  "BI-FAST service is unavailable",
                  "Layanan BI-FAST sedang tidak tersedia, silakan coba lagi",
                  cause);
        }
    }

    public static class QrisPaymentFailedException extends TransactionDomainException {
        public QrisPaymentFailedException(String reason) {
            super("TXN_EXT_004", 
                  "QRIS payment failed: " + reason,
                  "Pembayaran QRIS gagal");
        }
    }

    public static class QrisExpiredException extends TransactionDomainException {
        public QrisExpiredException(String qrId) {
            super("TXN_EXT_005", 
                  "QRIS code expired: " + qrId,
                  "Kode QRIS sudah kedaluwarsa");
        }
    }

    // === System Errors (6900-6999) ===
    
    public static class TransactionProcessingException extends TransactionDomainException {
        public TransactionProcessingException(String reason) {
            super("TXN_SYS_001", 
                  "Transaction processing error: " + reason,
                  "Terjadi kesalahan saat memproses transaksi");
        }

        public TransactionProcessingException(String reason, Throwable cause) {
            super("TXN_SYS_001", 
                  "Transaction processing error: " + reason,
                  "Terjadi kesalahan saat memproses transaksi",
                  cause);
        }
    }

    public static class TransactionTimeoutException extends TransactionDomainException {
        public TransactionTimeoutException(String transactionId) {
            super("TXN_SYS_002", 
                  "Transaction timeout: " + transactionId,
                  "Transaksi timeout, silakan cek status transaksi");
        }
    }
}
