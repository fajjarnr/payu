package id.payu.api.common.exception;

/**
 * Exception thrown when a dependency is unavailable and the request cannot be
 * served safely (fail-closed). Results in HTTP 503 Service Unavailable.
 * Used for rate-limit cache outages and identity-provider downtime on
 * authentication paths (LOGIN-005).
 */
public class ServiceUnavailableException extends BusinessException {

    public ServiceUnavailableException(String code, String message) {
        super(code, message);
    }

    public ServiceUnavailableException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
