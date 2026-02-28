package id.payu.gateway.domain.vo;

/**
 * Value Object representing HTTP methods.
 * Used for type safety in analytics and rate limiting.
 */
public enum HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH,
    HEAD,
    OPTIONS,
    TRACE,
    CONNECT;

    public static HttpMethod fromString(String method) {
        if (method == null || method.isBlank()) {
            return GET;
        }
        try {
            return valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            return GET;
        }
    }
}
