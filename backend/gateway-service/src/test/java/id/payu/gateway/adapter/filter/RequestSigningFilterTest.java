package id.payu.gateway.adapter.filter;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestProfile(RequestSigningFilterTestProfile.class)
@DisplayName("Request Signing Filter Tests")
public class RequestSigningFilterTest {

    private static final String TEST_PARTNER_ID = "partner-1";
    // pragma: allowlist secret
    private static final String TEST_SECRET_KEY = "dGVzdC1zZWNyZXQta2V5"; // base64 encoded "test-secret-key"

    @Test
    @DisplayName("Should accept valid signed request")
    public void testValidSignature() throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String signature = generateSignature("POST", "/api/v1/partners/test", timestamp, TEST_SECRET_KEY);

        given()
            .contentType("application/json")
            .header("X-Partner-Id", TEST_PARTNER_ID)
            .header("X-Signature", signature)
            .header("X-Timestamp", String.valueOf(timestamp))
            .when()
            .post("/api/v1/partners/test")
            .then()
            .statusCode(anyOf(is(200), is(201), is(404), is(503)));
    }

    @Test
    @DisplayName("Should reject request with missing signature")
    public void testMissingSignature() {
        given()
            .contentType("application/json")
            .header("X-Partner-Id", TEST_PARTNER_ID)
            .when()
            .post("/api/v1/partners/test")
            .then()
            .statusCode(401)
            .body("error", equalTo("MISSING_SIGNATURE"));
    }

    @Test
    @DisplayName("Should reject request with invalid signature")
    public void testInvalidSignature() {
        given()
            .contentType("application/json")
            .header("X-Partner-Id", TEST_PARTNER_ID)
            .header("X-Signature", "invalid-signature")
            .header("X-Timestamp", String.valueOf(Instant.now().getEpochSecond()))
            .when()
            .post("/api/v1/partners/test")
            .then()
            .statusCode(401)
            .body("error", equalTo("INVALID_SIGNATURE"));
    }

    @Test
    @DisplayName("Should reject request with old timestamp")
    public void testOldTimestamp() throws Exception {
        long oldTimestamp = Instant.now().getEpochSecond() - 1000;
        String signature = generateSignature("POST", "/api/v1/partners/test", oldTimestamp, TEST_SECRET_KEY);

        given()
            .contentType("application/json")
            .header("X-Partner-Id", TEST_PARTNER_ID)
            .header("X-Signature", signature)
            .header("X-Timestamp", String.valueOf(oldTimestamp))
            .when()
            .post("/api/v1/partners/test")
            .then()
            .statusCode(401)
            .body("error", equalTo("INVALID_TIMESTAMP"));
    }

    @Test
    @DisplayName("Should reject request from unknown partner")
    public void testUnknownPartner() throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String signature = generateSignature("POST", "/api/v1/partners/test", timestamp, TEST_SECRET_KEY);

        given()
            .contentType("application/json")
            .header("X-Partner-Id", "unknown-partner")
            .header("X-Signature", signature)
            .header("X-Timestamp", String.valueOf(timestamp))
            .when()
            .post("/api/v1/partners/test")
            .then()
            .statusCode(401)
            .body("error", equalTo("UNKNOWN_PARTNER"));
    }

    private String generateSignature(String method, String path, long timestamp, String secretKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);

        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(keyBytes, "HmacSHA256");
        mac.init(keySpec);

        String payload = method + "\n" + path + "\n" + timestamp + "\n";
        byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(signature);
    }
}
