package id.payu.auth.exception;

public class BiometricException extends RuntimeException {

    private final String errorCode;

    public BiometricException(String message) {
        super(message);
        this.errorCode = "BIO_000";
    }

    public BiometricException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BiometricException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
