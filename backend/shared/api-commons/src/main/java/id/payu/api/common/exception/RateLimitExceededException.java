package id.payu.api.common.exception;

/**
 * Exception thrown when rate limit is exceeded.
 * Results in HTTP 429 Too Many Requests response.
 */
public class RateLimitExceededException extends BusinessException {

    private final long retryAfterSeconds;

    /**
     * Creates a RateLimitExceededException with retry-after duration.
     *
     * @param retryAfterSeconds Seconds until the client can retry
     */
    public RateLimitExceededException(long retryAfterSeconds) {
        super("RATE_LIMIT_EXCEEDED", "Rate limit exceeded. Please retry after " + retryAfterSeconds + " seconds");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * Creates a RateLimitExceededException with custom message.
     *
     * @param message           Custom error message
     * @param retryAfterSeconds Seconds until the client can retry
     */
    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super("RATE_LIMIT_EXCEEDED", message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * Creates a RateLimitExceededException with code, message, and retry-after.
     *
     * @param code              Unique error code
     * @param message           Human-readable error message
     * @param retryAfterSeconds Seconds until the client can retry
     */
    public RateLimitExceededException(String code, String message, long retryAfterSeconds) {
        super(code, message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
