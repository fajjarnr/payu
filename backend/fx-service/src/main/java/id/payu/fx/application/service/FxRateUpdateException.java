package id.payu.fx.application.service;

public class FxRateUpdateException extends RuntimeException {
    public FxRateUpdateException(String message) {
        super(message);
    }

    public FxRateUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
