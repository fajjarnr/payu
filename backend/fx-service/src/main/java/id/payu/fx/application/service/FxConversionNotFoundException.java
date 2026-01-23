package id.payu.fx.application.service;

public class FxConversionNotFoundException extends RuntimeException {
    public FxConversionNotFoundException(String message) {
        super(message);
    }

    public FxConversionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
