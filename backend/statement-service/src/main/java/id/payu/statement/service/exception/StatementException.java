package id.payu.statement.service.exception;

import lombok.Getter;

/**
 * Custom exception for Statement service
 */
@Getter
public class StatementException extends RuntimeException {

    private final String errorCode;

    public StatementException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public StatementException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    // Error code constants
    public static final String STATEMENT_NOT_FOUND = "STATEMENT_002";
    public static final String STATEMENT_NOT_READY = "STATEMENT_003";
    public static final String STATEMENT_READ_FAILED = "STATEMENT_004";
    public static final String STATEMENT_GENERATION_FAILED = "STATEMENT_001";
}
