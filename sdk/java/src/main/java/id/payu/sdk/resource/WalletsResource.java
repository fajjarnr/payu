package id.payu.sdk.resource;

import id.payu.sdk.PayUClient;
import id.payu.sdk.config.PayUConfig;
import id.payu.sdk.error.PayUException;
import okhttp3.HttpUrl;
import okhttp3.Request;

/**
 * Resource client for wallet operations ({@code /api/v1/wallets}).
 */
public class WalletsResource {

    private final PayUClient client;
    private final PayUConfig config;

    public WalletsResource(PayUClient client, PayUConfig config) {
        this.client = client;
        this.config = config;
    }

    /**
     * Get wallet balance for an account.
     */
    public <T> T getBalance(String accountId, Class<T> responseType) throws PayUException {
        HttpUrl url = HttpUrl.get(config.getBaseUrl() + "/api/v1/wallets/" + accountId + "/balance");
        return client.execute(new Request.Builder().url(url).get().build(), responseType);
    }
}
