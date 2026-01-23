package id.payu.partner.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.TimeZone;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SnapBiSignatureService {

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    public String generateSignature(String clientSecret, String httpMethod, String endpoint, String accessToken, String requestBody, String timestamp) {
        try {
            String stringToSign = httpMethod + ":" + endpoint + ":" + accessToken + ":" + requestBody + ":" + timestamp;
            
            byte[] secretKeyBytes = clientSecret.getBytes(StandardCharsets.UTF_8);
            byte[] stringToSignBytes = stringToSign.getBytes(StandardCharsets.UTF_8);
            
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance(HMAC_SHA256_ALGORITHM);
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(secretKeyBytes, HMAC_SHA256_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(stringToSignBytes);
            
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate SNAP BI signature", e);
        }
    }

    public String generateSignatureWithClientKey(String clientSecret, String httpMethod, String endpoint, String timestamp, String requestBody) {
        try {
            String stringToSign = httpMethod + ":" + endpoint + ":" + timestamp + ":" + requestBody;
            
            byte[] secretKeyBytes = clientSecret.getBytes(StandardCharsets.UTF_8);
            byte[] stringToSignBytes = stringToSign.getBytes(StandardCharsets.UTF_8);
            
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance(HMAC_SHA256_ALGORITHM);
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(secretKeyBytes, HMAC_SHA256_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(stringToSignBytes);
            
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate SNAP BI signature with client key", e);
        }
    }

    public boolean validateSignature(String clientSecret, String httpMethod, String endpoint, String accessToken, String requestBody, String timestamp, String providedSignature) {
        String calculatedSignature = generateSignature(clientSecret, httpMethod, endpoint, accessToken, requestBody, timestamp);
        return MessageDigest.isEqual(calculatedSignature.getBytes(StandardCharsets.UTF_8), providedSignature.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateSignatureWithClientKey(String clientSecret, String httpMethod, String endpoint, String timestamp, String requestBody, String providedSignature) {
        String calculatedSignature = generateSignatureWithClientKey(clientSecret, httpMethod, endpoint, timestamp, requestBody);
        return MessageDigest.isEqual(calculatedSignature.getBytes(StandardCharsets.UTF_8), providedSignature.getBytes(StandardCharsets.UTF_8));
    }

    public String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    public String hashRequestBody(String requestBody) {
        if (requestBody == null || requestBody.isEmpty()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(requestBody.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash request body", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
