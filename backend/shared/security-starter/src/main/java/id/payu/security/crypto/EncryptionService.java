package id.payu.security.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;

/**
 * Service for field-level encryption/decryption
 * Uses AES-GCM for authenticated encryption
 */
@Slf4j
@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int KEY_LENGTH = 256;

    private final SecretKeySpec secretKey;
    private final ObjectMapper objectMapper;

    public EncryptionService(String encryptionKey) {
        // Derive a 256-bit key from the provided key string
        this.secretKey = deriveKey(encryptionKey);
        this.objectMapper = new ObjectMapper();
        log.info("Encryption Service initialized with AES-GCM");
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
     * Decrypt a string value
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            // Decode Base64
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            // Extract IV and encrypted data
            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] encryptedData = new byte[buffer.remaining()];
            buffer.get(encryptedData);

            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // Decrypt the data
            byte[] decryptedData = cipher.doFinal(encryptedData);

            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Failed to decrypt data", e);
        }
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

    /**
     * Derive a 256-bit key from the provided key string
     */
    private SecretKeySpec deriveKey(String keyString) {
        try {
            byte[] keyBytes = keyString.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            keyBytes = sha.digest(keyBytes);
            keyBytes = java.util.Arrays.copyOf(keyBytes, KEY_LENGTH / 8);
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
