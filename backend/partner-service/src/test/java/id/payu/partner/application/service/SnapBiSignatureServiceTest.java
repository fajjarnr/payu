package id.payu.partner.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class SnapBiSignatureServiceTest {

    @InjectMocks
    private SnapBiSignatureService signatureService;

    @Test
    public void testGenerateSignature() {
        String clientSecret = "test-secret-key";
        String httpMethod = "POST";
        String endpoint = "/v1/partner/auth/token";
        String timestamp = "2024-01-20T10:00:00Z";
        String requestBody = "{\"grantType\":\"client_credentials\"}";

        String signature = signatureService.generateSignatureWithClientKey(
            clientSecret, httpMethod, endpoint, timestamp, requestBody
        );

        assertNotNull(signature);
        assertFalse(signature.isEmpty());
    }

    @Test
    public void testValidateSignature() {
        String clientSecret = "test-secret-key";
        String httpMethod = "POST";
        String endpoint = "/v1/partner/auth/token";
        String timestamp = "2024-01-20T10:00:00Z";
        String requestBody = "{\"grantType\":\"client_credentials\"}";

        String signature = signatureService.generateSignatureWithClientKey(
            clientSecret, httpMethod, endpoint, timestamp, requestBody
        );

        boolean isValid = signatureService.validateSignatureWithClientKey(
            clientSecret, httpMethod, endpoint, timestamp, requestBody, signature
        );

        assertTrue(isValid);
    }

    @Test
    public void testValidateInvalidSignature() {
        String clientSecret = "test-secret-key";
        String httpMethod = "POST";
        String endpoint = "/v1/partner/auth/token";
        String timestamp = "2024-01-20T10:00:00Z";
        String requestBody = "{\"grantType\":\"client_credentials\"}";

        boolean isValid = signatureService.validateSignatureWithClientKey(
            clientSecret, httpMethod, endpoint, timestamp, requestBody, "invalid-signature"
        );

        assertFalse(isValid);
    }

    @Test
    public void testGetCurrentTimestamp() {
        String timestamp = signatureService.getCurrentTimestamp();
        assertNotNull(timestamp);
        assertTrue(timestamp.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
    }

    @Test
    public void testGenerateSignatureWithAccessToken() {
        String clientSecret = "test-secret-key";
        String httpMethod = "POST";
        String endpoint = "/v1/partner/payments";
        String accessToken = "test-access-token";
        String requestBody = "{\"partnerReferenceNo\":\"REF-123\"}";
        String timestamp = "2024-01-20T10:00:00Z";

        String signature = signatureService.generateSignature(
            clientSecret, httpMethod, endpoint, accessToken, requestBody, timestamp
        );

        assertNotNull(signature);
        assertFalse(signature.isEmpty());
    }

    @Test
    public void testSignatureUsesHmacSha512NotSha256() throws Exception {
        String clientSecret = "test-secret-key";
        String httpMethod = "POST";
        String endpoint = "/v1/partner/payments";
        String accessToken = "test-access-token";
        String requestBody = "{\"partnerReferenceNo\":\"REF-123\"}";
        String timestamp = "2024-01-20T10:00:00Z";

        // SNAP-BI mandates HMAC-SHA512 for symmetric signatures. A SHA-512 HMAC
        // is 64 bytes -> 88 Base64 chars; SHA-256 would be 44 chars.
        String signature = signatureService.generateSignature(
            clientSecret, httpMethod, endpoint, accessToken, requestBody, timestamp);
        assertEquals(88, signature.length(), "HMAC-SHA512 signature must be 88 base64 chars, got " + signature);

        // And it must match an independently computed HMAC-SHA512
        String hashedBody = signatureService.hashRequestBody(requestBody);
        String stringToSign = httpMethod + ":" + endpoint + ":" + accessToken + ":" + hashedBody + ":" + timestamp;
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA512");
        mac.init(new javax.crypto.spec.SecretKeySpec(clientSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA512"));
        String expected = java.util.Base64.getEncoder().encodeToString(mac.doFinal(stringToSign.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        assertEquals(expected, signature);
    }

    @Test
    public void testClientKeySignatureUsesHmacSha512() throws Exception {
        String clientSecret = "test-secret-key";
        String httpMethod = "POST";
        String endpoint = "/v1/partner/auth/token";
        String timestamp = "2024-01-20T10:00:00Z";
        String requestBody = "{\"grantType\":\"client_credentials\"}";

        String signature = signatureService.generateSignatureWithClientKey(
            clientSecret, httpMethod, endpoint, timestamp, requestBody);
        assertEquals(88, signature.length(), "HMAC-SHA512 signature must be 88 base64 chars, got " + signature);

        String hashedBody = signatureService.hashRequestBody(requestBody);
        String stringToSign = httpMethod + ":" + endpoint + ":" + timestamp + ":" + hashedBody;
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA512");
        mac.init(new javax.crypto.spec.SecretKeySpec(clientSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA512"));
        String expected = java.util.Base64.getEncoder().encodeToString(mac.doFinal(stringToSign.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        assertEquals(expected, signature);
    }
}
