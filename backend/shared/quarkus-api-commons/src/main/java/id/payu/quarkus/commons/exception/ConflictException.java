package id.payu.quarkus.commons.exception;

public class ConflictException extends BusinessException {

    public ConflictException(String code, String message) {
        super(code, message);
    }

    public static ConflictException duplicate(String resourceType, String identifier) {
        return new ConflictException("CONFLICT",
                String.format("%s with identifier '%s' already exists", resourceType, identifier));
    }

    public ConflictException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
