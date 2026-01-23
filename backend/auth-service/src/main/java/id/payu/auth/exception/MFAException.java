package id.payu.auth.exception;

public class MFAException extends RuntimeException {
    
    private final String errorCode;
    
    public MFAException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public MFAException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
