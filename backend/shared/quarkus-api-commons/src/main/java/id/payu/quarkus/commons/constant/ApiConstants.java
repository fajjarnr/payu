package id.payu.quarkus.commons.constant;

public final class ApiConstants {

    private ApiConstants() {
    }

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int MIN_PAGE_SIZE = 1;
    public static final String DEFAULT_SORT_DIRECTION = "DESC";
    public static final String DEFAULT_SORT_FIELD = "createdAt";

    public static final int DEFAULT_RATE_LIMIT_PER_MINUTE = 100;
    public static final int STRICT_RATE_LIMIT_PER_MINUTE = 10;
    public static final int DEFAULT_RATE_LIMIT_WINDOW_SECONDS = 60;

    public static final int DEFAULT_TIMEOUT_SECONDS = 30;
    public static final long FAST_TIMEOUT_MS = 1000;
    public static final long MEDIUM_TIMEOUT_MS = 5000;
    public static final long SLOW_TIMEOUT_MS = 30000;

    public static final int DEFAULT_MAX_RETRY_ATTEMPTS = 3;
    public static final long DEFAULT_RETRY_DELAY_MS = 1000;
    public static final long MAX_RETRY_DELAY_MS = 10000;

    public static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";
    public static final long IDEMPOTENCY_KEY_EXPIRATION_SECONDS = 86400;

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String DEVICE_ID_HEADER = "X-Device-ID";
    public static final String CLIENT_VERSION_HEADER = "X-Client-Version";
    public static final String CLIENT_PLATFORM_HEADER = "X-Client-Platform";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";
    public static final String DEFAULT_LANGUAGE = "id-ID";

    public static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    public static final String RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";
    public static final String RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
    public static final String RETRY_AFTER_HEADER = "Retry-After";

    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd";
    public static final String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String INDONESIAN_DATE_FORMAT = "dd-MM-yyyy";
    public static final String INDONESIAN_DATETIME_FORMAT = "dd-MM-yyyy HH:mm:ss";

    public static final String DEFAULT_CURRENCY = "IDR";
    public static final long MIN_TRANSFER_AMOUNT = 1000;
    public static final long MAX_BIFAST_AMOUNT = 250000000;

    public static final String PHONE_NUMBER_PATTERN = "^\\+628[1-9][0-9]{6,9}$";
    public static final String PHONE_NUMBER_PATTERN_ALT = "^628[1-9][0-9]{6,9}$";
    public static final String MOBILE_NUMBER_PATTERN = "^08[1-9][0-9]{6,9}$";

    public static final int NIK_LENGTH = 16;
    public static final String NIK_PATTERN = "^[0-9]{16}$";

    public static final int ACCOUNT_NUMBER_LENGTH = 10;
    public static final String ACCOUNT_NUMBER_PATTERN = "^[0-9]{10}$";

    public static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";
    public static final String UUID_PATTERN = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    public static final String API_VERSION = "v1";
    public static final String API_VERSION_PREFIX = "/api/v1/";

    public static final int REFERENCE_LENGTH = 16;
    public static final String REFERENCE_PATTERN = "^[A-Z0-9]{16}$";
}
