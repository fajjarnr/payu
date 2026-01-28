package id.payu.api.common.exception;

/**
 * Exception thrown when a requested resource is not found.
 * Results in HTTP 404 Not Found response.
 */
public class ResourceNotFoundException extends BusinessException {

    private final String resourceType;
    private final String resourceId;

    /**
     * Creates a ResourceNotFoundException for a resource type and ID.
     *
     * @param resourceType Type of resource (e.g., "Account", "Transaction")
     * @param resourceId   ID of the resource that was not found
     */
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super("NOT_FOUND", String.format("%s with id '%s' not found", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    /**
     * Creates a ResourceNotFoundException with a custom message.
     *
     * @param message Custom error message
     */
    public static ResourceNotFoundException withMessage(String message) {
        return new ResourceNotFoundException("NOT_FOUND", message, null, null);
    }

    /**
     * Creates a ResourceNotFoundException with code and message.
     *
     * @param code    Unique error code
     * @param message Human-readable error message
     */
    public static ResourceNotFoundException withCode(String code, String message) {
        return new ResourceNotFoundException(code, message, null, null);
    }

    /**
     * Internal constructor for all cases.
     */
    private ResourceNotFoundException(String code, String message, String resourceType, String resourceId) {
        super(code, message);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }
}
