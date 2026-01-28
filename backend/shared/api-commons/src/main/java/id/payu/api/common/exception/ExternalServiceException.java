package id.payu.api.common.exception;

/**
 * Exception thrown when an external service call fails.
 * Results in HTTP 502 Bad Gateway response.
 * Used for failures in BI-FAST, QRIS, Dukcapil, etc.
 */
public class ExternalServiceException extends BusinessException {

    private final String serviceName;
    private final String serviceError;

    /**
     * Creates an ExternalServiceException for a service.
     *
     * @param serviceName  Name of the external service
     * @param serviceError Error message from the service
     */
    public ExternalServiceException(String serviceName, String serviceError) {
        super("EXTERNAL_SERVICE_ERROR", String.format("External service '%s' returned an error: %s", serviceName, serviceError));
        this.serviceName = serviceName;
        this.serviceError = serviceError;
    }

    /**
     * Creates an ExternalServiceException with code and message.
     *
     * @param code         Unique error code (e.g., "TXN_EXT_BIFAST_001")
     * @param message      Human-readable error message
     * @param serviceName  Name of the external service
     * @param serviceError Error message from the service
     */
    public ExternalServiceException(String code, String message, String serviceName, String serviceError) {
        super(code, message);
        this.serviceName = serviceName;
        this.serviceError = serviceError;
    }

    /**
     * Creates an ExternalServiceException with code, message, and cause.
     *
     * @param code        Unique error code
     * @param message     Human-readable error message
     * @param cause       The cause of this exception
     * @param serviceName Name of the external service
     */
    public ExternalServiceException(String code, String message, Throwable cause, String serviceName) {
        super(code, message, cause);
        this.serviceName = serviceName;
        this.serviceError = cause != null ? cause.getMessage() : null;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceError() {
        return serviceError;
    }
}
