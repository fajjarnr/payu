package id.payu.security.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.codec.Hex;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Service for field-level encryption/decryption
 * Uses AES-GCM for authenticated encryption
 * 
 * <p>Note: This class is instantiated via {@link id.payu.security.config.SecurityAutoConfiguration}.
 * Do not add @Service annotation.</p>
 */
@Slf4j
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int KEY_LENGTH = 256;

    private final SecretKeySpec secretKey;
    private final List<SecretKeySpec> previousKeys;
    private final int currentKeyVersion;
    private final ObjectMapper objectMapper;
    private final byte[] pbkdf2Salt;

    public EncryptionService(String encryptionKey) {
        this(encryptionKey, Collections.emptyList(), null);
    }

    public EncryptionService(String encryptionKey, List<String> previousKeyStrings) {
        this(encryptionKey, previousKeyStrings, null);
    }

    /**
     * Construct with current key, optional previous keys for rotation, and optional salt.
     * Previous keys are used only for decryption of data encrypted with older keys.
     *
     * @param encryptionKey  Current encryption key (used for encrypt + decrypt)
     * @param previousKeys   Previous keys in reverse order (most recent first), used only for decryption fallback
     * @param salt           Optional PBKDF2 salt (null for default). MUST be externalized via Vault in production (BUG-BE-019).
     */
    public EncryptionService(String encryptionKey, List<String> previousKeys, String salt) {
        if (salt == null || salt.isEmpty()) {
            log.warn("╔══════════════════════════════════════════════════════════════════╗");
            log.warn("║  SECURITY WARNING: Using default PBKDF2 salt!                   ║");
            log.warn("║  Set payu.security.encryption.salt for production!              ║");
            log.warn("║  Default salt enables precomputation attacks.                   ║");
            log.warn("╚══════════════════════════════════════════════════════════════════╝");
        }
        this.pbkdf2Salt = (salt != null && !salt.isEmpty())
                ? salt.getBytes(StandardCharsets.UTF_8)
                : DEFAULT_PBKDF2_SALT;
        this.currentKeyVersion = previousKeys.size() + 1;
        this.secretKey = deriveKey(encryptionKey);
        this.previousKeys = new ArrayList<>();
        for (String prevKey : previousKeys) {
            this.previousKeys.add(deriveKey(prevKey));
        }
        this.objectMapper = new ObjectMapper();
        log.info("Encryption Service initialized with AES-GCM (key version: {}, {} previous keys available for rotation)",
                currentKeyVersion, previousKeys.size());
    }

    /**
     * Encrypt a string value
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // Initialize cipher for encryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // Encrypt the plaintext
            byte[] encryptedData = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Combine IV and encrypted data
            byte[] combined = ByteBuffer.allocate(iv.length + encryptedData.length)
                    .put(iv)
                    .put(encryptedData)
                    .array();

            // Return as Base64 encoded string
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    /**
     * Decrypt a string value.
     * Tries the current key first, then falls back to previous keys for key rotation support.
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        // Try current key first
        try {
            return decryptWithKey(encryptedText, secretKey);
        } catch (Exception e) {
            // If current key fails and we have previous keys, try them
            for (int i = 0; i < previousKeys.size(); i++) {
                try {
                    String result = decryptWithKey(encryptedText, previousKeys.get(i));
                    log.info("Decrypted with previous key version {} — consider re-encrypting with current key",
                            currentKeyVersion - i - 1);
                    return result;
                } catch (Exception ignored) {
                    // Try next key
                }
            }
            log.error("Decryption failed with all available keys");
            throw new RuntimeException("Failed to decrypt data — no matching key found", e);
        }
    }

    private String decryptWithKey(String encryptedText, SecretKeySpec key) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedText);

        ByteBuffer buffer = ByteBuffer.wrap(combined);
        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);
        byte[] encryptedData = new byte[buffer.remaining()];
        buffer.get(encryptedData);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

        byte[] decryptedData = cipher.doFinal(encryptedData);
        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    /**
     * Re-encrypt a value with the current key.
     * Use this during key rotation to migrate data encrypted with previous keys.
     *
     * @param encryptedText  Data encrypted with any known key version
     * @return Data re-encrypted with the current key
     */
    public String reEncrypt(String encryptedText) {
        String plainText = decrypt(encryptedText);
        return encrypt(plainText);
    }

    /**
     * Encrypt specified fields in a JSON object
     */
    public String encryptFields(String jsonString, java.util.List<String> fieldsToEncrypt) {
        if (jsonString == null || jsonString.isEmpty()) {
            return jsonString;
        }

        try {
            JsonNode node = objectMapper.readTree(jsonString);
            if (node.isObject()) {
                ObjectNode objectNode = (ObjectNode) node;
                encryptFieldsInObject(objectNode, fieldsToEncrypt);
                return objectMapper.writeValueAsString(objectNode);
            }
            return jsonString;
        } catch (Exception e) {
            log.error("Failed to encrypt fields in JSON", e);
            return jsonString;
        }
    }

    /**
     * Decrypt specified fields in a JSON object
     */
    public String decryptFields(String jsonString, java.util.List<String> fieldsToDecrypt) {
        if (jsonString == null || jsonString.isEmpty()) {
            return jsonString;
        }

        try {
            JsonNode node = objectMapper.readTree(jsonString);
            if (node.isObject()) {
                ObjectNode objectNode = (ObjectNode) node;
                decryptFieldsInObject(objectNode, fieldsToDecrypt);
                return objectMapper.writeValueAsString(objectNode);
            }
            return jsonString;
        } catch (Exception e) {
            log.error("Failed to decrypt fields in JSON", e);
            return jsonString;
        }
    }

    private void encryptFieldsInObject(ObjectNode objectNode, java.util.List<String> fieldsToEncrypt) {
        Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode fieldValue = field.getValue();

            // Check if field should be encrypted
            boolean shouldEncrypt = fieldsToEncrypt.stream()
                    .anyMatch(pattern -> fieldName.matches(pattern));

            if (shouldEncrypt && fieldValue.isTextual()) {
                String encryptedValue = encrypt(fieldValue.asText());
                objectNode.put(fieldName, encryptedValue);
            } else if (fieldValue.isObject()) {
                encryptFieldsInObject((ObjectNode) fieldValue, fieldsToEncrypt);
            } else if (fieldValue.isArray()) {
                for (JsonNode arrayItem : fieldValue) {
                    if (arrayItem.isObject()) {
                        encryptFieldsInObject((ObjectNode) arrayItem, fieldsToEncrypt);
                    }
                }
            }
        }
    }

    private void decryptFieldsInObject(ObjectNode objectNode, java.util.List<String> fieldsToDecrypt) {
        Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode fieldValue = field.getValue();

            // Check if field should be decrypted
            boolean shouldDecrypt = fieldsToDecrypt.stream()
                    .anyMatch(pattern -> fieldName.matches(pattern));

            if (shouldDecrypt && fieldValue.isTextual()) {
                try {
                    String decryptedValue = decrypt(fieldValue.asText());
                    objectNode.put(fieldName, decryptedValue);
                } catch (Exception e) {
                    // If decryption fails, leave the value as is
                    log.debug("Failed to decrypt field {}, might not be encrypted", fieldName);
                }
            } else if (fieldValue.isObject()) {
                decryptFieldsInObject((ObjectNode) fieldValue, fieldsToDecrypt);
            } else if (fieldValue.isArray()) {
                for (JsonNode arrayItem : fieldValue) {
                    if (arrayItem.isObject()) {
                        decryptFieldsInObject((ObjectNode) arrayItem, fieldsToDecrypt);
                    }
                }
            }
        }
    }

    private static final int PBKDF2_ITERATIONS = 600_000;

    /**
     * Default PBKDF2 salt — 48 characters for adequate entropy.
     * BUG-SHARED-003 FIX: Increased from "PayUDefaultSalt2024!" (20 chars) to a longer,
     * more complex default. Still MUST be overridden in production via
     * {@code payu.security.encryption.salt} property and externalized via Vault.
     */
    private static final byte[] DEFAULT_PBKDF2_SALT =
            "PayU-AES256-PBKDF2-Key-Derivation-Salt-v2-2026!".getBytes(StandardCharsets.UTF_8);

    /**
     * Derive a 256-bit key from the provided key string using PBKDF2.
     * Uses PBKDF2WithHmacSHA256 with 600k iterations per OWASP 2024 guidance.
     */
    private SecretKeySpec deriveKey(String keyString) {
        try {
            KeySpec spec = new PBEKeySpec(
                    keyString.toCharArray(),
                    this.pbkdf2Salt,
                    PBKDF2_ITERATIONS,
                    KEY_LENGTH
            );
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            log.error("Failed to derive encryption key", e);
            throw new RuntimeException("Failed to derive encryption key", e);
        }
    }

    /**
     * Encrypt a value for storage in database
     */
    public String encryptForDatabase(String plainText) {
        return "ENC(" + encrypt(plainText) + ")";
    }

    /**
     * Decrypt a value from database
     */
    public String decryptFromDatabase(String encryptedText) {
        if (encryptedText != null && encryptedText.startsWith("ENC(") && encryptedText.endsWith(")")) {
            String actualValue = encryptedText.substring(4, encryptedText.length() - 1);
            return decrypt(actualValue);
        }
        return encryptedText;
    }
}
