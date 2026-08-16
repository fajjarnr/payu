package id.payu.sdk.resource;

import id.payu.sdk.PayUClient;
import id.payu.sdk.config.PayUConfig;
import id.payu.sdk.error.PayUException;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Resource client for transfers ({@code /api/v1/transactions/transfer}).
 */
public class TransfersResource {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final PayUClient client;
    private final PayUConfig config;

    public TransfersResource(PayUClient client, PayUConfig config) {
        this.client = client;
        this.config = config;
    }

    /**
     * Initiate a transfer.
     *
     * @param body JSON request body matching {@code POST /api/v1/transactions/transfer}
     * @param idempotencyKey required for financial mutations
     */
    public <T> T create(String body, String idempotencyKey, Class<T> responseType) throws PayUException {
        HttpUrl url = HttpUrl.get(config.getBaseUrl() + "/api/v1/transactions/transfer");
        Request.Builder builder = new Request.Builder().url(url)
                .post(RequestBody.create(body, JSON));
        if (idempotencyKey != null) {
            builder.header("X-Idempotency-Key", idempotencyKey);
        }
        return client.execute(builder.build(), responseType);
    }

    /**
     * Get transfer status by transaction ID.
     */
    public <T> T getStatus(String id, Class<T> responseType) throws PayUException {
        HttpUrl url = HttpUrl.get(config.getBaseUrl() + "/api/v1/transactions/" + id);
        return client.execute(new Request.Builder().url(url).get().build(), responseType);
    }
}
