package id.payu.lending.exception;

public class LendingDomainException extends RuntimeException {

    public LendingDomainException(String message) {
        super(message);
    }

    public LendingDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
