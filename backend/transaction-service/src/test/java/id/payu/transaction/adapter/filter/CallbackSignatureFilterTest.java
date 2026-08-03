package id.payu.transaction.adapter.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CallbackSignatureFilter}.
 *
 * <p>BUG-TRANS-CALLBACK-001 + BUG-VA-CALLBACK-001: callback endpoints
 * must require HMAC-SHA256 signature verification. This test enforces
 * that requirement by directly testing the filter.</p>
 */
class CallbackSignatureFilterTest {

    private static final String SECRET = "test-callback-secret-key-do-not-use-in-prod";
    private static final List<String> PATHS = List.of(
            "/api/v1/disbursements/callback",
            "/api/v1/payments/va/callback");

    private CallbackSignatureFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CallbackSignatureFilter(SECRET, 300, PATHS, true);
    }

    @Test
    @DisplayName("should reject callback without X-Signature header")
    void shouldRejectCallbackWithoutSignature() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/disbursements/callback");
        request.setContent("{\"status\":\"COMPLETED\"}".getBytes(StandardCharsets.UTF_8));
        request.addHeader("X-Timestamp", String.valueOf(Instant.now().getEpochSecond()));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("MISSING_SIGNATURE");
    }

    @Test
    @DisplayName("should reject callback without X-Timestamp header")
    void shouldRejectCallbackWithoutTimestamp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/disbursements/callback");
        request.setContent("{\"status\":\"COMPLETED\"}".getBytes(StandardCharsets.UTF_8));
        request.addHeader("X-Signature", "abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("MISSING_TIMESTAMP");
    }

    @Test
    @DisplayName("should reject callback with expired timestamp")
    void shouldRejectCallbackWithExpiredTimestamp() throws Exception {
        long expired = Instant.now().getEpochSecond() - 600; // 10 min ago, tolerance is 5 min
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/disbursements/callback");
        request.setContent("{\"status\":\"COMPLETED\"}".getBytes(StandardCharsets.UTF_8));
        request.addHeader("X-Timestamp", String.valueOf(expired));
        request.addHeader("X-Signature", "abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("TIMESTAMP_EXPIRED");
    }

    @Test
    @DisplayName("should reject callback with invalid signature")
    void shouldRejectCallbackWithInvalidSignature() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/disbursements/callback");
        request.setContent("{\"status\":\"COMPLETED\"}".getBytes(StandardCharsets.UTF_8));
        request.addHeader("X-Timestamp", String.valueOf(Instant.now().getEpochSecond()));
        request.addHeader("X-Signature", "deadbeef" + "0".repeat(56)); // 64 hex chars but wrong
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("INVALID_SIGNATURE");
    }

    @Test
    @DisplayName("should allow callback with valid signature")
    void shouldAllowCallbackWithValidSignature() throws Exception {
        String body = "{\"status\":\"COMPLETED\"}";
        long timestamp = Instant.now().getEpochSecond();
        String signature = computeHmac(timestamp, body, SECRET);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/disbursements/callback");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        request.addHeader("X-Timestamp", String.valueOf(timestamp));
        request.addHeader("X-Signature", signature);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        // The filter should have forwarded to the chain
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("should reject callback for unconfigured path")
    void shouldAllowRequestForUnprotectedPath() throws Exception {
        // Even WITHOUT signature, an unprotected path should pass through
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/disbursements");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("should reject callback when server secret not configured")
    void shouldRejectCallbackWhenServerSecretNotConfigured() throws Exception {
        CallbackSignatureFilter filterNoSecret = new CallbackSignatureFilter("", 300, PATHS, true);
        String body = "{\"status\":\"COMPLETED\"}";
        long timestamp = Instant.now().getEpochSecond();

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/disbursements/callback");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        request.addHeader("X-Timestamp", String.valueOf(timestamp));
        request.addHeader("X-Signature", "anyvalue");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filterNoSecret.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("SERVER_MISCONFIGURED");
    }

    @Test
    @DisplayName("should bypass filter when disabled")
    void shouldBypassFilterWhenDisabled() throws Exception {
        CallbackSignatureFilter filterDisabled = new CallbackSignatureFilter(SECRET, 300, PATHS, false);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/disbursements/callback");
        request.setContent("{\"status\":\"COMPLETED\"}".getBytes(StandardCharsets.UTF_8));
        // No signature headers
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filterDisabled.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("should reject callback for VA callback path with invalid signature")
    void shouldRejectVaCallback() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/payments/va/callback");
        request.setContent("{\"vaNumber\":\"123\",\"amount\":1000}".getBytes(StandardCharsets.UTF_8));
        request.addHeader("X-Timestamp", String.valueOf(Instant.now().getEpochSecond()));
        request.addHeader("X-Signature", "wrongsignature");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    private String computeHmac(long timestamp, String body, String secret) throws Exception {
        String stringToSign = timestamp + "\n" + body;
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] sigBytes = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(sigBytes);
    }
}
