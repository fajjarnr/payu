package id.payu.lending.exception;

public class RepaymentProcessingException extends RuntimeException {
    public RepaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
