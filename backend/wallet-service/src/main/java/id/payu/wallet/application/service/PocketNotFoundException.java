package id.payu.wallet.application.service;

public class PocketNotFoundException extends RuntimeException {
    public PocketNotFoundException(String message) {
        super(message);
    }

    public PocketNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
