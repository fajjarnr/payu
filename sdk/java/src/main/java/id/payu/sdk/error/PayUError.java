package id.payu.sdk.error;

import okhttp3.Response;

import java.io.IOException;

/**
 * Wraps a non-2xx API response into a typed SDK exception.
 */
public class PayUError extends PayUException {

    private final int statusCode;
    private final String code;
    private final String body;

    public PayUError(int statusCode, String code, String message, String body) {
        super(message);
        this.statusCode = statusCode;
        this.code = code;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getCode() {
        return code;
    }

    public String getBody() {
        return body;
    }

    /**
     * Convert an unsuccessful OkHttp response into a PayUError.
     */
    public static PayUError fromResponse(Response response) throws PayUException {
        String body;
        try {
            body = response.body() != null ? response.body().string() : "";
        } catch (IOException e) {
            throw new PayUException("Failed to read error response: " + e.getMessage(), e);
        }
        int status = response.code();
        return new PayUError(status, "HTTP_" + status, "API error with status " + status, body);
    }
}
