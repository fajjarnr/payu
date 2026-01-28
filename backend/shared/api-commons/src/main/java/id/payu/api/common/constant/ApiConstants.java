package id.payu.api.common.constant;

/**
 * Common API constants for PayU Digital Banking Platform.
 * Includes pagination limits, timeouts, and other API-related constants.
 */
public final class ApiConstants {

    private ApiConstants() {
    }

    // ==================== PAGINATION ====================

    /**
     * Default page number (0-based).
     */
    public static final int DEFAULT_PAGE = 0;

    /**
     * Default page size for list endpoints.
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Maximum page size allowed.
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * Minimum page size allowed.
     */
    public static final int MIN_PAGE_SIZE = 1;

    /**
     * Default sort direction.
     */
    public static final String DEFAULT_SORT_DIRECTION = "DESC";

    /**
     * Default sort field.
     */
    public static final String DEFAULT_SORT_FIELD = "createdAt";

    // ==================== RATE LIMITING ====================

    /**
     * Default rate limit requests per minute.
     */
    public static final int DEFAULT_RATE_LIMIT_PER_MINUTE = 100;

    /**
     * Strict rate limit for sensitive operations (login, transfer).
     */
    public static final int STRICT_RATE_LIMIT_PER_MINUTE = 10;

    /**
     * Default rate limit window in seconds.
     */
    public static final int DEFAULT_RATE_LIMIT_WINDOW_SECONDS = 60;

    // ==================== TIME LIMITS ====================

    /**
     * Default timeout for external service calls in seconds.
     */
    public static final int DEFAULT_TIMEOUT_SECONDS = 30;

    /**
     * Timeout for fast operations in milliseconds.
     */
    public static final long FAST_TIMEOUT_MS = 1000;

    /**
     * Timeout for medium operations in milliseconds.
     */
    public static final long MEDIUM_TIMEOUT_MS = 5000;

    /**
     * Timeout for slow operations in milliseconds.
     */
    public static final long SLOW_TIMEOUT_MS = 30000;

    // ==================== RETRY CONFIGURATION ====================

    /**
     * Default maximum retry attempts.
     */
    public static final int DEFAULT_MAX_RETRY_ATTEMPTS = 3;

    /**
     * Default retry delay in milliseconds.
     */
    public static final long DEFAULT_RETRY_DELAY_MS = 1000;

    /**
     * Maximum retry delay in milliseconds.
     */
    public static final long MAX_RETRY_DELAY_MS = 10000;

    // ==================== IDEMPOTENCY ====================

    /**
     * Idempotency key header name.
     */
    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    /**
     * Idempotency key expiration in seconds (24 hours).
     */
    public static final long IDEMPOTENCY_KEY_EXPIRATION_SECONDS = 86400;

    // ==================== REQUEST HEADERS ====================

    /**
     * Request ID header name.
     */
    public static final String REQUEST_ID_HEADER = "X-Request-ID";

    /**
     * Correlation ID header name.
     */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    /**
     * Device ID header name.
     */
    public static final String DEVICE_ID_HEADER = "X-Device-ID";

    /**
     * Client version header name.
     */
    public static final String CLIENT_VERSION_HEADER = "X-Client-Version";

    /**
     * Client platform header name.
     */
    public static final String CLIENT_PLATFORM_HEADER = "X-Client-Platform";

    /**
     * Authorization header name.
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Bearer token prefix.
     */
    public static final String BEARER_PREFIX = "Bearer ";

    /**
     * Accept language header name.
     */
    public static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";

    /**
     * Default language.
     */
    public static final String DEFAULT_LANGUAGE = "id-ID";

    // ==================== RESPONSE HEADERS ====================

    /**
     * Rate limit remaining header name.
     */
    public static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";

    /**
     * Rate limit reset header name.
     */
    public static final String RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";

    /**
     * Rate limit limit header name.
     */
    public static final String RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";

    /**
     * Retry after header name.
     */
    public static final String RETRY_AFTER_HEADER = "Retry-After";

    /**
     * Deprecation header name.
     */
    public static final String DEPRECATION_HEADER = "Deprecation";

    /**
     * Sunset header name.
     */
    public static final String SUNSET_HEADER = "Sunset";

    /**
     * Link header name.
     */
    public static final String LINK_HEADER = "Link";

    // ==================== DATE FORMATS ====================

    /**
     * ISO 8601 date format.
     */
    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd";

    /**
     * ISO 8601 datetime format.
     */
    public static final String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    /**
     * Indonesian date format.
     */
    public static final String INDONESIAN_DATE_FORMAT = "dd-MM-yyyy";

    /**
     * Indonesian datetime format.
     */
    public static final String INDONESIAN_DATETIME_FORMAT = "dd-MM-yyyy HH:mm:ss";

    // ==================== CURRENCY ====================

    /**
     * Default currency code.
     */
    public static final String DEFAULT_CURRENCY = "IDR";

    /**
     * Minimum transfer amount.
     */
    public static final long MIN_TRANSFER_AMOUNT = 1000;

    /**
     * Maximum transfer amount (BI-FAST limit).
     */
    public static final long MAX_BIFAST_AMOUNT = 250000000;

    // ==================== PHONE NUMBER ====================

    /**
     * Indonesian phone number pattern.
     */
    public static final String PHONE_NUMBER_PATTERN = "^\\+628[1-9][0-9]{6,9}$";

    /**
     * Alternative Indonesian phone number pattern (without +).
     */
    public static final String PHONE_NUMBER_PATTERN_ALT = "^628[1-9][0-9]{6,9}$";

    /**
     * Mobile number pattern (08xx).
     */
    public static final String MOBILE_NUMBER_PATTERN = "^08[1-9][0-9]{6,9}$";

    // ==================== NIK (Indonesian ID) ====================

    /**
     * NIK length.
     */
    public static final int NIK_LENGTH = 16;

    /**
     * NIK pattern.
     */
    public static final String NIK_PATTERN = "^[0-9]{16}$";

    // ==================== ACCOUNT NUMBER ====================

    /**
     * Account number length.
     */
    public static final int ACCOUNT_NUMBER_LENGTH = 10;

    /**
     * Account number pattern.
     */
    public static final String ACCOUNT_NUMBER_PATTERN = "^[0-9]{10}$";

    // ==================== EMAIL ====================

    /**
     * Email pattern.
     */
    public static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";

    // ==================== UUID ====================

    /**
     * UUID pattern.
     */
    public static final String UUID_PATTERN = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    // ==================== API VERSION ====================

    /**
     * Current API version.
     */
    public static final String API_VERSION = "v1";

    /**
     * API version prefix.
     */
    public static final String API_VERSION_PREFIX = "/api/v1/";

    // ==================== TRANSACTION REFERENCE ====================

    /**
     * Transaction reference number length.
     */
    public static final int REFERENCE_LENGTH = 16;

    /**
     * Transaction reference pattern.
     */
    public static final String REFERENCE_PATTERN = "^[A-Z0-9]{16}$";
}
