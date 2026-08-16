package id.payu.sdk.auth;

import id.payu.sdk.config.PayUConfig;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Adds HMAC-SHA256 request signing headers to every request.
 *
 * <p>Signs {@code METHOD|PATH|TIMESTAMP|BODY_HASH} with the API secret, matching
 * the TS SDK and gateway signature scheme.
 */
public class AuthInterceptor implements Interceptor {

    private final PayUConfig config;

    public AuthInterceptor(PayUConfig config) {
        this.config = config;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

        Request signed = request.newBuilder()
                .header("X-API-Key", config.getApiKey())
                .header("X-Timestamp", timestamp)
                .header("X-Signature", signature(request, timestamp))
                .build();
        return chain.proceed(signed);
    }

    private String signature(Request request, String timestamp) throws IOException {
        String method = request.method().toUpperCase();
        HttpUrl url = request.url();
        String path = url.encodedPath();
        String body = request.body() != null ? request.body().toString() : "";
        String stringToSign = method + "|" + path + "|" + timestamp + "|" + hashBody(body);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(config.getApiSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | java.security.InvalidKeyException e) {
            throw new IOException("Failed to sign request", e);
        }
    }

    private String hashBody(String body) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
}
