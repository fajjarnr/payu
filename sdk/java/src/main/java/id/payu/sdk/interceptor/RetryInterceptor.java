package id.payu.sdk.interceptor;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/**
 * Retries idempotent-safe failures (5xx, network errors) with a bounded number
 * of attempts. Never retries 4xx responses.
 */
public class RetryInterceptor implements Interceptor {

    private final int maxRetries;

    public RetryInterceptor(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = null;
        IOException lastFailure = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (response != null) {
                    response.close();
                }
                response = chain.proceed(request);
                if (!response.isSuccessful() && response.code() >= 500) {
                    continue;
                }
                return response;
            } catch (IOException e) {
                lastFailure = e;
            }
        }
        if (response != null) {
            return response;
        }
        throw lastFailure;
    }
}
