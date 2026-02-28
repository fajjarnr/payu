package id.payu.gateway.domain.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BodyMaskingRule value object.
 */
class BodyMaskingRuleTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateDefaultMaskingRule() {
        BodyMaskingRule rule = BodyMaskingRule.defaultMasking();

        assertFalse(rule.getFieldsToMask().isEmpty());
        assertTrue(rule.getFieldsToMask().contains("password"));
        assertTrue(rule.getFieldsToMask().contains("pin"));
        assertTrue(rule.getFieldsToMask().contains("cvv"));
    }

    @Test
    void shouldCreatePartialMaskingRule() {
        BodyMaskingRule rule = BodyMaskingRule.partialMasking(Set.of("email", "phone"));

        assertEquals(2, rule.getFieldsToMask().size());
        assertTrue(rule.getFieldsToMask().contains("email"));
        assertTrue(rule.getFieldsToMask().contains("phone"));
    }

    @Test
    void shouldMaskSensitiveFields() {
        BodyMaskingRule rule = BodyMaskingRule.defaultMasking();

        String json = "{\"username\":\"john\",\"password\":\"secret123\",\"email\":\"john@example.com\"}";
        String masked = rule.applyMasking(json, objectMapper);

        assertTrue(masked.contains("\"username\":\"john\""));
        assertTrue(masked.contains("\"password\":\"***\""));
        assertTrue(masked.contains("\"email\":\"john@example.com\""));
    }

    @Test
    void shouldMaskNestedFields() {
        BodyMaskingRule rule = BodyMaskingRule.defaultMasking();

        String json = "{\"user\":{\"name\":\"john\",\"password\":\"secret\"},\"data\":\"value\"}";
        String masked = rule.applyMasking(json, objectMapper);

        assertTrue(masked.contains("\"name\":\"john\""));
        assertTrue(masked.contains("\"password\":\"***\""));
    }

    @Test
    void shouldMaskFieldsInArray() {
        BodyMaskingRule rule = BodyMaskingRule.defaultMasking();

        String json = "[{\"id\":1,\"password\":\"secret1\"},{\"id\":2,\"password\":\"secret2\"}]";
        String masked = rule.applyMasking(json, objectMapper);

        assertTrue(masked.contains("\"id\":1"));
        assertTrue(masked.contains("\"id\":2"));
        assertTrue(masked.contains("\"password\":\"***\""));
    }

    @Test
    void shouldReturnOriginalForNonJson() {
        BodyMaskingRule rule = BodyMaskingRule.defaultMasking();

        String notJson = "This is not JSON";
        String result = rule.applyMasking(notJson, objectMapper);

        assertEquals(notJson, result);
    }

    @Test
    void shouldReturnOriginalForNullBody() {
        BodyMaskingRule rule = BodyMaskingRule.defaultMasking();

        String result = rule.applyMasking(null, objectMapper);

        assertNull(result);
    }

    @Test
    void shouldReturnOriginalForEmptyBody() {
        BodyMaskingRule rule = BodyMaskingRule.defaultMasking();

        String result = rule.applyMasking("", objectMapper);

        assertEquals("", result);
    }

    @Test
    void shouldReturnOriginalWhenNoFieldsToMask() {
        BodyMaskingRule rule = new BodyMaskingRule(Set.of(),
            BodyMaskingRule.MaskingStrategy.FULL, "***");

        String json = "{\"password\":\"secret\"}";
        String result = rule.applyMasking(json, objectMapper);

        assertEquals(json, result);
    }

    @Test
    void shouldApplyFullMaskingStrategy() {
        BodyMaskingRule rule = new BodyMaskingRule(Set.of("secret"),
            BodyMaskingRule.MaskingStrategy.FULL, "[REDACTED]");

        String json = "{\"secret\":\"my-secret-value\"}";
        String masked = rule.applyMasking(json, objectMapper);

        assertTrue(masked.contains("\"secret\":\"[REDACTED]\""));
    }

    @Test
    void shouldApplyPartialMaskingStrategy() {
        BodyMaskingRule rule = new BodyMaskingRule(Set.of("cardNumber"),
            BodyMaskingRule.MaskingStrategy.PARTIAL, null);

        String json = "{\"cardNumber\":\"1234567890123456\"}";
        String masked = rule.applyMasking(json, objectMapper);

        assertTrue(masked.contains("\"cardNumber\":\"12***56\""));
    }

    @Test
    void shouldApplyLast4MaskingStrategy() {
        BodyMaskingRule rule = new BodyMaskingRule(Set.of("cardNumber"),
            BodyMaskingRule.MaskingStrategy.LAST_4, null);

        String json = "{\"cardNumber\":\"1234567890123456\"}";
        String masked = rule.applyMasking(json, objectMapper);

        assertTrue(masked.contains("\"cardNumber\":\"****3456\""));
    }

    @Test
    void shouldApplyHashMaskingStrategy() {
        BodyMaskingRule rule = new BodyMaskingRule(Set.of("secret"),
            BodyMaskingRule.MaskingStrategy.HASH, null);

        String json = "{\"secret\":\"my-secret\"}";
        String masked = rule.applyMasking(json, objectMapper);

        assertTrue(masked.contains("\"secret\":\"[HASH:"));
    }
}
