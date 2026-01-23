package id.payu.auth.dto;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

public record BiometricRegistration(
        String registrationId,
        String username,
        String deviceId,
        String deviceType,
        String publicKey,
        Instant createdAt,
        boolean active
) {
}
