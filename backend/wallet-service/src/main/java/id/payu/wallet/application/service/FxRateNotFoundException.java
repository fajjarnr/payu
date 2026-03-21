package id.payu.wallet.application.service;

// TODO BUG-ARCH-002: Migrate to extend BusinessException with proper error codes
public class FxRateNotFoundException extends RuntimeException {
    public FxRateNotFoundException(String message) {
        super(message);
    }

    public FxRateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
