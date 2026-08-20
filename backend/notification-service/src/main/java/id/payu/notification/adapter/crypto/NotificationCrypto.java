package id.payu.notification.adapter.crypto;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@ApplicationScoped
public class NotificationCrypto {

    private static final Logger LOG = Logger.getLogger(NotificationCrypto.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int KEY_LENGTH = 256;
    private static final int PBKDF2_ITERATIONS = 600_000;
    private static final byte[] DEFAULT_SALT = "PayU-AES256-PBKDF2-Key-Derivation-Salt-v2-2026!".getBytes(StandardCharsets.UTF_8);

    @ConfigProperty(name = "payu.encryption.key", defaultValue = "CHANGE-ME-IN-PRODUCTION-payu-dev-key-2026")
    String encryptionKey;

    @ConfigProperty(name = "payu.encryption.salt", defaultValue = "PayU-AES256-PBKDF2-Key-Derivation-Salt-v2-2026!")
    String salt;

    private SecretKeySpec secretKey;

    SecretKeySpec getKey() {
        if (secretKey != null) return secretKey;
        byte[] saltBytes = (salt != null && !salt.isBlank()) ? salt.getBytes(StandardCharsets.UTF_8) : DEFAULT_SALT;
        try {
            PBEKeySpec spec = new PBEKeySpec(encryptionKey.toCharArray(), saltBytes, PBKDF2_ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            secretKey = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive encryption key", e);
        }
        return secretKey;
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) return plain;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] combined = ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            LOG.errorf("Encryption failed: %s", e.getMessage());
            throw new RuntimeException("Failed to encrypt", e);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) return cipherText;
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);
            if (combined.length <= GCM_IV_LENGTH) return cipherText;
            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // ponytail: backward compat — plaintext stored before encryption returns as-is
            return cipherText;
        }
    }
}
