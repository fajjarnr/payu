package id.payu.compliance.exception;

public class ComplianceDomainException extends RuntimeException {
    public ComplianceDomainException(String message) {
        super(message);
    }

    public ComplianceDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
