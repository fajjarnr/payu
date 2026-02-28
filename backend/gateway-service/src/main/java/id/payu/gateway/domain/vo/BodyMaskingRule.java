package id.payu.gateway.domain.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

/**
 * Value Object representing body field masking rules.
 * Immutable and thread-safe.
 */
public class BodyMaskingRule {

    private final Set<String> fieldsToMask;
    private final MaskingStrategy strategy;
    private final String maskPattern;

    public BodyMaskingRule(Set<String> fieldsToMask, MaskingStrategy strategy, String maskPattern) {
        this.fieldsToMask = Collections.unmodifiableSet(new HashSet<>(
            Objects.requireNonNull(fieldsToMask, "Fields to mask cannot be null")));
        this.strategy = Objects.requireNonNull(strategy, "Strategy cannot be null");
        this.maskPattern = maskPattern != null ? maskPattern : "***";
    }

    public static BodyMaskingRule defaultMasking() {
        return new BodyMaskingRule(
            Set.of("password", "pin", "cvv", "token", "secret", "apiKey"),
            MaskingStrategy.FULL,
            "***"
        );
    }

    public static BodyMaskingRule partialMasking(Set<String> fields) {
        return new BodyMaskingRule(fields, MaskingStrategy.PARTIAL, null);
    }

    public Set<String> getFieldsToMask() {
        return fieldsToMask;
    }

    public MaskingStrategy getStrategy() {
        return strategy;
    }

    /**
     * Apply masking to a JSON body.
     */
    public String applyMasking(String jsonBody, ObjectMapper objectMapper) {
        if (jsonBody == null || jsonBody.isBlank() || fieldsToMask.isEmpty()) {
            return jsonBody;
        }

        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            int maskedCount = maskFields(root);

            if (maskedCount > 0) {
                return objectMapper.writeValueAsString(root);
            }
            return jsonBody;
        } catch (Exception e) {
            // Return original if parsing fails
            return jsonBody;
        }
    }

    private int maskFields(JsonNode node) {
        if (node == null) return 0;

        int removed = 0;

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<String> fieldNames = objectNode.fieldNames();

            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldValue = objectNode.get(fieldName);

                if (fieldsToMask.contains(fieldName)) {
                    String maskedValue = strategy.mask(fieldValue.asText(), maskPattern);
                    objectNode.set(fieldName, TextNode.valueOf(maskedValue));
                    removed++;
                } else {
                    removed += maskFields(fieldValue);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                removed += maskFields(element);
            }
        }

        return removed;
    }

    public enum MaskingStrategy {
        FULL {
            @Override
            public String mask(String value, String pattern) {
                return pattern != null ? pattern : "***";
            }
        },
        PARTIAL {
            @Override
            public String mask(String value, String pattern) {
                if (value == null || value.length() <= 4) {
                    return "***";
                }
                // Show first 2 and last 2 characters
                return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
            }
        },
        LAST_4 {
            @Override
            public String mask(String value, String pattern) {
                if (value == null || value.length() <= 4) {
                    return "***";
                }
                return "****" + value.substring(value.length() - 4);
            }
        },
        HASH {
            @Override
            public String mask(String value, String pattern) {
                return "[HASH:" + Integer.toHexString(Objects.hash(value)) + "]";
            }
        };

        public abstract String mask(String value, String pattern);
    }
}
