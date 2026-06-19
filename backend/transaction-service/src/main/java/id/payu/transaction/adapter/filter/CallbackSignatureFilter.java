package id.payu.transaction.adapter.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

/**
 * HMAC-SHA256 signature verification filter for callback endpoints.
 *
 * <p>BUG-TRANS-CALLBACK-001 + BUG-VA-CALLBACK-001: External callback endpoints
 * (BI-FAST, bank VA confirmation) were previously protected only by
 * {@code .anyRequest().authenticated()} which means any valid JWT could invoke
 * them. An attacker with any user JWT could mark disbursements as completed or
 * mark VA payments as received.</p>
 *
 * <p>This filter enforces HMAC-SHA256 signature verification for callback paths.
 * The caller (BI-FAST / bank) signs the request body with a shared secret; we
 * verify the signature before allowing the request through.</p>
 *
 * <h3>Signature scheme</h3>
 * <pre>
 * stringToSign = unixTimestamp + "\n" + requestBody
 * signature    = hex(HMAC-SHA256(secret, stringToSign))
 * </pre>
 * Headers required:
 * <ul>
 *   <li>{@code X-Signature}: hex-encoded HMAC-SHA256</li>
 *   <li>{@code X-Timestamp}: unix epoch seconds (max 5 min drift)</li>
 * </ul>
 *
 * <h3>Configuration</h3>
 * <ul>
 *   <li>{@code payu.callback.signature.secret} — shared secret (env: PAYU_CALLBACK_SECRET)</li>
 *   <li>{@code payu.callback.signature.tolerance-seconds} — max timestamp drift (default 300)</li>
 *   <li>{@code payu.callback.signature.paths} — path patterns to protect (default: callback endpoints)</li>
 *   <li>{@code payu.callback.signature.enabled} — toggle (default: true)</li>
 * </ul>
 */
@Component
public class CallbackSignatureFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CallbackSignatureFilter.class);

    public static final String SIGNATURE_HEADER = "X-Signature";
    public static final String TIMESTAMP_HEADER = "X-Timestamp";
    public static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String secret;
    private final long toleranceSeconds;
    private final List<String> protectedPaths;
    private final boolean enabled;

    public CallbackSignatureFilter(
            @Value("${payu.callback.signature.secret:}") String secret,
            @Value("${payu.callback.signature.tolerance-seconds:300}") long toleranceSeconds,
            @Value("${payu.callback.signature.paths:/api/v1/disbursements/callback,/api/v1/virtual-accounts/callback}") List<String> protectedPaths,
            @Value("${payu.callback.signature.enabled:true}") boolean enabled) {
        this.secret = secret;
        this.toleranceSeconds = toleranceSeconds;
        this.protectedPaths = protectedPaths;
        this.enabled = enabled;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String path = request.getRequestURI();
        return protectedPaths.stream().noneMatch(path::equals);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Cache body so it can be read again by the controller after signature verification
        byte[] bodyBytes = StreamUtils.copyToByteArray(request.getInputStream());
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request, bodyBytes);

        String body = new String(bodyBytes, StandardCharsets.UTF_8);
        String providedSignature = cachedRequest.getHeader(SIGNATURE_HEADER);
        String timestampStr = cachedRequest.getHeader(TIMESTAMP_HEADER);

        if (providedSignature == null || providedSignature.isBlank()) {
            log.warn("Callback rejected: missing {} header for {}",
                    SIGNATURE_HEADER, cachedRequest.getRequestURI());
            sendUnauthorized(response, "MISSING_SIGNATURE", "X-Signature header is required");
            return;
        }

        if (timestampStr == null || timestampStr.isBlank()) {
            log.warn("Callback rejected: missing {} header for {}",
                    TIMESTAMP_HEADER, cachedRequest.getRequestURI());
            sendUnauthorized(response, "MISSING_TIMESTAMP", "X-Timestamp header is required");
            return;
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            log.warn("Callback rejected: invalid timestamp format {}", timestampStr);
            sendUnauthorized(response, "INVALID_TIMESTAMP_FORMAT", "X-Timestamp must be unix epoch seconds");
            return;
        }

        long now = Instant.now().getEpochSecond();
        long diff = Math.abs(now - timestamp);
        if (diff > toleranceSeconds) {
            log.warn("Callback rejected: timestamp {} seconds out of tolerance (max {})",
                    diff, toleranceSeconds);
            sendUnauthorized(response, "TIMESTAMP_EXPIRED",
                    "Request timestamp outside tolerance window of " + toleranceSeconds + " seconds");
            return;
        }

        if (secret == null || secret.isBlank()) {
            log.error("Callback rejected: PAYU_CALLBACK_SECRET not configured");
            sendUnauthorized(response, "SERVER_MISCONFIGURED",
                    "Callback signature secret is not configured on the server");
            return;
        }

        String expectedSignature;
        try {
            expectedSignature = calculateSignature(timestampStr, body, secret);
        } catch (Exception e) {
            log.error("Callback rejected: failed to calculate expected signature", e);
            sendUnauthorized(response, "SIGNATURE_ERROR", "Failed to verify signature");
            return;
        }

        // Constant-time comparison
        byte[] expectedBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
        byte[] providedBytes = providedSignature.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedBytes, providedBytes)) {
            log.warn("Callback rejected: signature mismatch for {} (path={})",
                    cachedRequest.getRequestURI(), cachedRequest.getRequestURI());
            sendUnauthorized(response, "INVALID_SIGNATURE", "Signature verification failed");
            return;
        }

        log.debug("Callback signature verified for {}", cachedRequest.getRequestURI());
        chain.doFilter(cachedRequest, response);
    }

    private String calculateSignature(String timestamp, String body, String secretKey) throws Exception {
        String stringToSign = timestamp + "\n" + body;
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        mac.init(keySpec);
        byte[] signatureBytes = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(signatureBytes);
    }

    private void sendUnauthorized(HttpServletResponse response, String error, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\":\"" + error + "\",\"message\":\"" + message + "\"}");
    }
}

/**
 * HttpServletRequest wrapper that caches the body for re-reading.
 * Required so the controller can parse the body after the filter has consumed it.
 */
class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request, byte[] cachedBody) {
        super(request);
        this.cachedBody = cachedBody;
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyServletInputStream(this.cachedBody);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }
}

class CachedBodyServletInputStream extends ServletInputStream {
    private final ByteArrayInputStream buffer;

    public CachedBodyServletInputStream(byte[] body) {
        this.buffer = new ByteArrayInputStream(body);
    }

    @Override
    public boolean isFinished() {
        return buffer.available() == 0;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setReadListener(ReadListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int read() {
        return buffer.read();
    }
}
