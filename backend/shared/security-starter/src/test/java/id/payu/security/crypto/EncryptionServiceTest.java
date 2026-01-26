package id.payu.security.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EncryptionService
 */
class EncryptionServiceTest {

    private static final String TEST_KEY = "test-encryption-key-for-unit-testing";
    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService(TEST_KEY);
    }

    @Test
    void testEncryptAndDecrypt() {
        String plainText = "Hello, World!";
        String encrypted = encryptionService.encrypt(plainText);
        String decrypted = encryptionService.decrypt(encrypted);

        assertNotNull(encrypted);
        assertNotNull(decrypted);
        assertEquals(plainText, decrypted);
        assertNotEquals(plainText, encrypted);
    }

    @Test
    void testEncryptNull() {
        String result = encryptionService.encrypt(null);
        assertNull(result);
    }

    @Test
    void testEncryptEmpty() {
        String result = encryptionService.encrypt("");
        assertEquals("", result);
    }

    @Test
    void testDecryptNull() {
        String result = encryptionService.decrypt(null);
        assertNull(result);
    }

    @Test
    void testDecryptEmpty() {
        String result = encryptionService.decrypt("");
        assertEquals("", result);
    }

    @Test
    void testEncryptDifferentResults() {
        String plainText = "Same text";
        String encrypted1 = encryptionService.encrypt(plainText);
        String encrypted2 = encryptionService.encrypt(plainText);

        // Each encryption should produce different result due to random IV
        assertNotEquals(encrypted1, encrypted2);

        // But both should decrypt to the same original text
        assertEquals(plainText, encryptionService.decrypt(encrypted1));
        assertEquals(plainText, encryptionService.decrypt(encrypted2));
    }

    @Test
    void testEncryptFields() {
        String json = "{\"name\":\"John\",\"email\":\"john@example.com\",\"ssn\":\"123-45-6789\"}";
        List<String> fieldsToEncrypt = List.of("ssn");

        String result = encryptionService.encryptFields(json, fieldsToEncrypt);

        assertNotNull(result);
        assertNotEquals(json, result);
        // The ssn field should be encrypted (Base64-like string)
        assertTrue(result.contains("\"ssn\":"));
        assertFalse(result.contains("123-45-6789"));
    }

    @Test
    void testDecryptFields() {
        // First encrypt a field
        String json = "{\"name\":\"John\",\"email\":\"john@example.com\",\"ssn\":\"123-45-6789\"}";
        List<String> fieldsToEncrypt = List.of("ssn");
        String encryptedJson = encryptionService.encryptFields(json, fieldsToEncrypt);

        // Now decrypt it back
        List<String> fieldsToDecrypt = List.of("ssn");
        String result = encryptionService.decryptFields(encryptedJson, fieldsToDecrypt);

        // Should get back the original value
        assertNotNull(result);
        assertTrue(result.contains("123-45-6789"));
    }

    @Test
    void testEncryptFieldsNull() {
        String result = encryptionService.encryptFields(null, List.of("field"));
        assertNull(result);
    }

    @Test
    void testEncryptFieldsEmpty() {
        String result = encryptionService.encryptFields("", List.of("field"));
        assertEquals("", result);
    }

    @Test
    void testEncryptFieldsNoMatch() {
        String json = "{\"name\":\"John\",\"email\":\"john@example.com\"}";
        List<String> fieldsToEncrypt = List.of("ssn");

        String result = encryptionService.encryptFields(json, fieldsToEncrypt);

        // No fields should match, so result should be the same
        assertEquals(json, result);
    }

    @Test
    void testEncryptForDatabase() {
        String plainText = "Sensitive Data";
        String result = encryptionService.encryptForDatabase(plainText);

        assertTrue(result.startsWith("ENC("));
        assertTrue(result.endsWith(")"));
    }

    @Test
    void testDecryptFromDatabase() {
        String plainText = "Sensitive Data";
        String encrypted = encryptionService.encrypt(plainText);
        String dbFormat = "ENC(" + encrypted + ")";

        String result = encryptionService.decryptFromDatabase(dbFormat);

        assertEquals(plainText, result);
    }

    @Test
    void testDecryptFromDatabaseNonEncrypted() {
        String plainText = "Not Encrypted";
        String result = encryptionService.decryptFromDatabase(plainText);

        assertEquals(plainText, result);
    }

    @Test
    void testDecryptFromDatabaseNull() {
        String result = encryptionService.decryptFromDatabase(null);
        assertNull(result);
    }

    @Test
    void testSpecialCharacters() {
        String plainText = "Test with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?";
        String encrypted = encryptionService.encrypt(plainText);
        String decrypted = encryptionService.decrypt(encrypted);

        assertEquals(plainText, decrypted);
    }

    @Test
    void testUnicodeCharacters() {
        String plainText = "Test with unicode: Indonesian Rupiah Rp 1.000.000, and emoji 🇮🇩💰";
        String encrypted = encryptionService.encrypt(plainText);
        String decrypted = encryptionService.decrypt(encrypted);

        assertEquals(plainText, decrypted);
    }

    @Test
    void testLongText() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longText.append("This is a long text. ");
        }
        String plainText = longText.toString();
        String encrypted = encryptionService.encrypt(plainText);
        String decrypted = encryptionService.decrypt(encrypted);

        assertEquals(plainText, decrypted);
    }

    @Test
    void testDecryptInvalidBase64() {
        String invalidBase64 = "not-valid-base64!!!";

        Exception exception = assertThrows(RuntimeException.class, () -> {
            encryptionService.decrypt(invalidBase64);
        });

        assertTrue(exception.getMessage().contains("Failed to decrypt"));
    }

    @Test
    void testEncryptFieldsNestedJson() {
        String json = "{\"user\":{\"name\":\"John\",\"ssn\":\"123-45-6789\"},\"address\":{\"zip\":\"12345\"}}";
        List<String> fieldsToEncrypt = List.of("ssn", "zip");

        String result = encryptionService.encryptFields(json, fieldsToEncrypt);

        assertNotNull(result);
        assertNotEquals(json, result);
        // The original values should be encrypted (not visible)
        assertFalse(result.contains("123-45-6789"));
        assertFalse(result.contains("12345"));
    }

    @Test
    void testEncryptFieldsJsonArray() {
        String json = "{\"users\":[{\"name\":\"John\",\"ssn\":\"111-11-1111\"},{\"name\":\"Jane\",\"ssn\":\"222-22-2222\"}]}";
        List<String> fieldsToEncrypt = List.of("ssn");

        String result = encryptionService.encryptFields(json, fieldsToEncrypt);

        assertNotNull(result);
        assertNotEquals(json, result);
    }
}
