package id.payu.promotion.adapter.client;

/**
 * Exception thrown when wallet credit operations fail.
 */
public class WalletCreditException extends RuntimeException {

    public WalletCreditException(String message) {
        super(message);
    }

    public WalletCreditException(String message, Throwable cause) {
        super(message, cause);
    }
}
