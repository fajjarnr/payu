package id.payu.shared.restclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.net.URI;

/**
 * Centralized REST client error handler that maps HTTP error responses
 * to PayU domain exceptions.
 *
 * <p>This handler differentiates between:
 * <ul>
 *   <li>4xx Client errors — not retryable (except 429 Too Many Requests)</li>
 *   <li>5xx Server errors — retryable</li>
 *   <li>429 Too Many Requests — retryable with backoff</li>
 * </ul>
 */
public class RestClientErrorHandler implements ResponseErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(RestClientErrorHandler.class);

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().isError();
    }

    @Override
    public void handleError(URI url, org.springframework.http.HttpMethod method,
                            ClientHttpResponse response) throws IOException {
        int statusCode = response.getStatusCode().value();
        String statusText = response.getStatusText();
        String requestInfo = (url != null && method != null)
                ? method + " " + url
                : "unknown request";

        log.error("REST client error: {} {} - status={} reason={}",
                method, url, statusCode, statusText);

        if (statusCode == 429) {
            throw new ExternalServiceUnavailableException(
                    "EXT_RATE_LIMITED",
                    String.format("Rate limited by external service: %s (HTTP 429)", requestInfo));
        }

        if (statusCode >= 500) {
            throw new ExternalServiceUnavailableException(
                    "EXT_SERVER_ERROR",
                    String.format("External service error: %s (HTTP %d: %s)", requestInfo, statusCode, statusText));
        }

        if (statusCode == 401 || statusCode == 403) {
            throw new ExternalServiceClientException(
                    "EXT_AUTH_ERROR",
                    String.format("Authentication/authorization failed: %s (HTTP %d)", requestInfo, statusCode));
        }

        if (statusCode == 404) {
            throw new ExternalServiceClientException(
                    "EXT_NOT_FOUND",
                    String.format("External resource not found: %s (HTTP 404)", requestInfo));
        }

        if (statusCode >= 400) {
            throw new ExternalServiceClientException(
                    "EXT_CLIENT_ERROR",
                    String.format("Client error calling external service: %s (HTTP %d: %s)",
                            requestInfo, statusCode, statusText));
        }
    }

    /**
     * Exception indicating an external service is unavailable or returned a server error.
     * These are retryable by the circuit breaker/retry mechanism.
     */
    public static class ExternalServiceUnavailableException extends IOException {
        private final String code;

        public ExternalServiceUnavailableException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    /**
     * Exception indicating a client error when calling an external service.
     * These are NOT retryable (the request itself is wrong).
     */
    public static class ExternalServiceClientException extends IOException {
        private final String code;

        public ExternalServiceClientException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
