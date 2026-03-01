package id.payu.statement.application.service.exception;

/**
 * Exception for receipt-related errors.
 * Epic E-19: Transaction Proof & Receipts (IMP-055)
 */
public class ReceiptException extends RuntimeException {

    private final String errorCode;

    public ReceiptException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ReceiptException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
