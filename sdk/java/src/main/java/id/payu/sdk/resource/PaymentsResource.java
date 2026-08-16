package id.payu.sdk.resource;

import id.payu.sdk.PayUClient;
import id.payu.sdk.config.PayUConfig;
import id.payu.sdk.error.PayUException;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Resource client for bill payments ({@code /api/v1/payments}).
 */
public class PaymentsResource {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final PayUClient client;
    private final PayUConfig config;

    public PaymentsResource(PayUClient client, PayUConfig config) {
        this.client = client;
        this.config = config;
    }

    /**
     * Create a bill payment.
     *
     * @param body JSON request body matching {@code POST /api/v1/payments}
     * @param idempotencyKey required for financial mutations
     */
    public <T> T create(String body, String idempotencyKey, Class<T> responseType) throws PayUException {
        HttpUrl url = HttpUrl.get(config.getBaseUrl() + "/api/v1/payments");
        Request.Builder builder = new Request.Builder().url(url)
                .post(RequestBody.create(body, JSON));
        if (idempotencyKey != null) {
            builder.header("X-Idempotency-Key", idempotencyKey);
        }
        return client.execute(builder.build(), responseType);
    }

    /**
     * Get payment status by ID.
     */
    public <T> T getStatus(String id, Class<T> responseType) throws PayUException {
        HttpUrl url = HttpUrl.get(config.getBaseUrl() + "/api/v1/payments/" + id);
        return client.execute(new Request.Builder().url(url).get().build(), responseType);
    }
}
