package id.payu.wallet.application.service;

// TODO BUG-ARCH-002: Migrate to extend BusinessException with proper error codes
public class PocketNotFoundException extends RuntimeException {
    public PocketNotFoundException(String message) {
        super(message);
    }

    public PocketNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
