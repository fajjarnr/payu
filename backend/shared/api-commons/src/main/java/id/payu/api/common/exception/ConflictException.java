package id.payu.api.common.exception;

/**
 * Exception thrown when a request conflicts with existing state.
 * Results in HTTP 409 Conflict response.
 * Used for duplicate resources, state conflicts, etc.
 */
public class ConflictException extends BusinessException {

    /**
     * Creates a ConflictException with code and message.
     *
     * @param code    Unique error code
     * @param message Human-readable error message
     */
    public ConflictException(String code, String message) {
        super(code, message);
    }

    /**
     * Creates a ConflictException for duplicate resource.
     *
     * @param resourceType Type of resource
     * @param identifier   Identifier that caused the conflict
     */
    public static ConflictException duplicate(String resourceType, String identifier) {
        return new ConflictException("CONFLICT",
                String.format("%s with identifier '%s' already exists", resourceType, identifier));
    }

    /**
     * Creates a ConflictException with code, message, and cause.
     *
     * @param code    Unique error code
     * @param message Human-readable error message
     * @param cause   The cause of this exception
     */
    public ConflictException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
