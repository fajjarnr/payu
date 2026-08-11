package id.payu.backoffice.adapter.persistence;

import static org.junit.jupiter.api.Assertions.*;

import id.payu.backoffice.adapter.persistence.entity.KycReviewEntity;
import id.payu.security.annotation.Sensitive;
import id.payu.security.converter.EncryptedStringConverter;
import id.payu.security.crypto.BlindIndexService;
import jakarta.persistence.Convert;
import org.junit.jupiter.api.Test;

class KycEncryptionMappingTest {
    @Test
    void piiFieldsUseSharedEncryptionConverterAndSensitivityMarker() throws Exception {
        for (String fieldName : new String[]{"userId", "accountNumber", "documentNumber", "fullName", "address", "phoneNumber"}) {
            var field = KycReviewEntity.class.getDeclaredField(fieldName);
            assertNotNull(field.getAnnotation(Sensitive.class), fieldName);
            assertEquals(EncryptedStringConverter.class, field.getAnnotation(Convert.class).converter(), fieldName);
        }
    }

    @Test
    void blindIndexPreservesCaseAndRejectsNonCanonicalWhitespace() {
        BlindIndexService service = new BlindIndexService("01234567890123456789012345678901", "");
        String index = service.index("User-123");
        assertNotEquals(index, service.index("user-123"));
        assertThrows(IllegalArgumentException.class, () -> service.index(" User-123 "));
        assertFalse(index.contains("user-123"));
        assertEquals(64, index.length());
    }

    @Test
    void lookupSupportsPreviousKeyDuringRotation() {
        String previous = "previous-blind-index-key-000000001";
        BlindIndexService rotating = new BlindIndexService("current-blind-index-key-0000000001", previous);
        BlindIndexService old = new BlindIndexService(previous, "");

        assertTrue(rotating.lookupIndexes("user-123").contains(old.index("user-123")));
        assertEquals(rotating.index("user-123"), rotating.lookupIndexes("user-123").getFirst());
    }

    @Test
    void exposesCurrentKeyVersionForRotationBackfill() {
        BlindIndexService service = new BlindIndexService(
                "current-blind-index-key-0000000001", "v2",
                "v1=previous-blind-index-key-000000001");
        assertEquals("v2", service.currentVersion());
        assertEquals(2, service.lookupIndexes("user-123").size());
    }
}
