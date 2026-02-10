package id.payu.statement.application.service.exception;

import id.payu.api.common.exception.BusinessException;
import lombok.Getter;

/**
 * Custom exception for Statement service
 */
@Getter
public class StatementException extends BusinessException {

    public StatementException(String errorCode, String message) {
        super(errorCode, message);
    }

    public StatementException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    // Error code constants
    public static final String STATEMENT_NOT_FOUND = "STATEMENT_002";
    public static final String STATEMENT_NOT_READY = "STATEMENT_003";
    public static final String STATEMENT_READ_FAILED = "STATEMENT_004";
    public static final String STATEMENT_GENERATION_FAILED = "STATEMENT_001";
}
