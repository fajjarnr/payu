package id.payu.quarkus.commons.exception;

public class ExternalServiceException extends BusinessException {

    private final String serviceName;
    private final String serviceError;

    public ExternalServiceException(String serviceName, String serviceError) {
        super("EXTERNAL_SERVICE_ERROR", String.format("External service '%s' returned an error: %s", serviceName, serviceError));
        this.serviceName = serviceName;
        this.serviceError = serviceError;
    }

    public ExternalServiceException(String code, String message, String serviceName, String serviceError) {
        super(code, message);
        this.serviceName = serviceName;
        this.serviceError = serviceError;
    }

    public ExternalServiceException(String code, String message, Throwable cause, String serviceName) {
        super(code, message, cause);
        this.serviceName = serviceName;
        this.serviceError = cause != null ? cause.getMessage() : null;
    }

    public String getServiceName() { return serviceName; }
    public String getServiceError() { return serviceError; }
}
