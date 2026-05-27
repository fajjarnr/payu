package id.payu.quarkus.commons.exception;

public class ResourceNotFoundException extends BusinessException {

    private final String resourceType;
    private final String resourceId;

    public ResourceNotFoundException(String resourceType, String resourceId) {
        super("NOT_FOUND", String.format("%s with id '%s' not found", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    private ResourceNotFoundException(String code, String message, String resourceType, String resourceId) {
        super(code, message);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public static ResourceNotFoundException withMessage(String message) {
        return new ResourceNotFoundException("NOT_FOUND", message, null, null);
    }

    public static ResourceNotFoundException withCode(String code, String message) {
        return new ResourceNotFoundException(code, message, null, null);
    }

    public String getResourceType() { return resourceType; }
    public String getResourceId() { return resourceId; }
}
