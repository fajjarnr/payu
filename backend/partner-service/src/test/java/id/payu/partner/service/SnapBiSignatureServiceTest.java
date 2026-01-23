package id.payu.partner.service;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class SnapBiSignatureServiceTest {

    @jakarta.inject.Inject
    SnapBiSignatureService signatureService;

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
}
