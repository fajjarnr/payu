package id.payu.api.common.exception;

import lombok.Getter;

import java.util.List;

/**
 * Base exception for all business rule violations in PayU services.
 * All business exceptions should extend this class to maintain consistent error handling.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final transient Object[] args;

    /**
     * Creates a BusinessException with code and message.
     *
     * @param code    Unique error code (e.g., "ACC_001", "TXN_VAL_001")
     * @param message Human-readable error message
     */
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.args = null;
    }

    /**
     * Creates a BusinessException with code, message, and cause.
     *
     * @param code    Unique error code
     * @param message Human-readable error message
     * @param cause   The cause of this exception
     */
    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.args = null;
    }

    /**
     * Creates a BusinessException with code, message, and arguments for message formatting.
     *
     * @param code    Unique error code
     * @param message Human-readable error message (may contain placeholders)
     * @param args    Arguments to format into the message
     */
    public BusinessException(String code, String message, Object... args) {
        super(message);
        this.code = code;
        this.args = args;
    }

    /**
     * Creates a BusinessException with code, message, cause, and arguments.
     *
     * @param code    Unique error code
     * @param message Human-readable error message
     * @param cause   The cause of this exception
     * @param args    Arguments to format into the message
     */
    public BusinessException(String code, String message, Throwable cause, Object... args) {
        super(message, cause);
        this.code = code;
        this.args = args;
    }
}
