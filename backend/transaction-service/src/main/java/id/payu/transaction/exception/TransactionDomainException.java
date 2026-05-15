package id.payu.transaction.exception;

import id.payu.api.common.exception.BusinessException;
import id.payu.api.common.exception.ConflictException;
import id.payu.api.common.exception.ExternalServiceException;
import id.payu.api.common.exception.InsufficientFundsException;

/**
 * Base exception for TransactionEntity Service domain errors.
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
public abstract class TransactionDomainException extends BusinessException {

    protected TransactionDomainException(String code, String message) {
        super(code, message);
    }

    protected TransactionDomainException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }

    protected TransactionDomainException(String code, String message, Object... args) {
        super(code, message, args);
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

    public static class DuplicateTransactionException extends ConflictException {
        public DuplicateTransactionException(String referenceId) {
            super("TXN_BUS_001",
                  "Duplicate transaction detected: " + referenceId);
        }
    }

    public static class TransactionLimitExceededException extends TransactionDomainException {
        public TransactionLimitExceededException(String limitType) {
            super("TXN_BUS_002",
                  "TransactionEntity limit exceeded: " + limitType,
                  "Batas transaksi terlampaui");
        }
    }

    public static class TransactionNotAllowedException extends TransactionDomainException {
        public TransactionNotAllowedException(String reason) {
            super("TXN_BUS_003",
                  "TransactionEntity not allowed: " + reason,
                  "Transaksi tidak diizinkan");
        }
    }

    public static class TransferToSelfException extends ConflictException {
        public TransferToSelfException() {
            super("TXN_BUS_004",
                  "Cannot transfer to own account");
        }
    }

    // === Balance/Wallet Errors (6200-6299) ===

    public static class TxnInsufficientBalanceException extends InsufficientFundsException {
        public TxnInsufficientBalanceException(String accountId) {
            super("TXN_BAL_001",
                  "Insufficient balance for account: " + accountId);
        }

        public TxnInsufficientBalanceException(String accountId, java.math.BigDecimal required,
                                               java.math.BigDecimal available) {
            super("TXN_BAL_001",
                  "Insufficient balance for account: " + accountId,
                  accountId,
                  required,
                  available);
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
                  cause);
        }
    }

    public static class WalletServiceUnavailableException extends ExternalServiceException {
        public WalletServiceUnavailableException() {
            super("TXN_BAL_003",
                  "Wallet service is unavailable",
                  "Layanan wallet sedang tidak tersedia, silakan coba lagi",
                  "WalletService");
        }

        public WalletServiceUnavailableException(Throwable cause) {
            super("TXN_BAL_003",
                  "Wallet service is unavailable",
                  cause,
                  "WalletService");
        }
    }

    // === External Service Errors (6300-6399) ===

    public static class BiFastTransferFailedException extends ExternalServiceException {
        public BiFastTransferFailedException(String reason) {
            super("TXN_EXT_001",
                  "BI-FAST transfer failed: " + reason,
                  "Transfer BI-FAST gagal",
                  "BI-FAST");
        }

        public BiFastTransferFailedException(String reason, Throwable cause) {
            super("TXN_EXT_001",
                  "BI-FAST transfer failed: " + reason,
                  cause,
                  "BI-FAST");
        }
    }

    public static class BiFastAccountInquiryFailedException extends ExternalServiceException {
        public BiFastAccountInquiryFailedException(String reason) {
            super("TXN_EXT_002",
                  "BI-FAST account inquiry failed: " + reason,
                  "Gagal mengecek rekening tujuan",
                  "BI-FAST");
        }
    }

    public static class BiFastServiceUnavailableException extends ExternalServiceException {
        public BiFastServiceUnavailableException() {
            super("TXN_EXT_003",
                  "BI-FAST service is unavailable",
                  "Layanan BI-FAST sedang tidak tersedia, silakan coba lagi",
                  "BI-FAST");
        }

        public BiFastServiceUnavailableException(Throwable cause) {
            super("TXN_EXT_003",
                  "BI-FAST service is unavailable",
                  cause,
                  "BI-FAST");
        }
    }

    public static class QrisPaymentFailedException extends ExternalServiceException {
        public QrisPaymentFailedException(String reason) {
            super("TXN_EXT_004",
                  "QRIS payment failed: " + reason,
                  "Pembayaran QRIS gagal",
                  "QRIS");
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
                  "TransactionEntity processing error: " + reason,
                  "Terjadi kesalahan saat memproses transaksi");
        }

        public TransactionProcessingException(String reason, Throwable cause) {
            super("TXN_SYS_001",
                  "TransactionEntity processing error: " + reason,
                  cause);
        }
    }

    public static class TransactionTimeoutException extends TransactionDomainException {
        public TransactionTimeoutException(String transactionId) {
            super("TXN_SYS_002",
                  "TransactionEntity timeout: " + transactionId,
                  "Transaksi timeout, silakan cek status transaksi");
        }
    }
}
