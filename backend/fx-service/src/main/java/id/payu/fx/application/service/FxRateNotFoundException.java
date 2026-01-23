package id.payu.fx.application.service;

public class FxRateNotFoundException extends RuntimeException {
    public FxRateNotFoundException(String message) {
        super(message);
    }

    public FxRateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
