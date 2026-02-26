package id.payu.gateway.adapter.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import id.payu.gateway.config.GatewayConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DisplayName("ResponseMaskingFilter Unit Tests")
class ResponseMaskingFilterTest {

    private ResponseMaskingFilter filter;
    private ObjectMapper objectMapper;
    private GatewayConfig config;
    private GatewayConfig.ResponseMaskingConfig maskingConfig;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        config = Mockito.mock(GatewayConfig.class);
        maskingConfig = Mockito.mock(GatewayConfig.ResponseMaskingConfig.class);
        when(config.responseMasking()).thenReturn(maskingConfig);
        when(maskingConfig.enabled()).thenReturn(true);
        when(maskingConfig.blacklistedFields()).thenReturn(
                List.of("stackTrace", "internalErrorCode", "traceId", "spanId", "debugInfo",
                        "internalId", "serverHost", "dbQuery")
        );
        when(maskingConfig.maskedPaths()).thenReturn(
                List.of("/api/v1/partners", "/v1/partner")
        );

        filter = new ResponseMaskingFilter();
        try {
            var configField = ResponseMaskingFilter.class.getDeclaredField("config");
            configField.setAccessible(true);
            configField.set(filter, config);

            var omField = ResponseMaskingFilter.class.getDeclaredField("objectMapper");
            omField.setAccessible(true);
            omField.set(filter, objectMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        filter.init();
    }

    @Nested
    @DisplayName("Path Matching")
    class PathMatching {

        @Test
        @DisplayName("should mask partner paths")
        void shouldMaskPartnerPaths() {
            assertTrue(filter.shouldMask("/api/v1/partners"));
            assertTrue(filter.shouldMask("/api/v1/partners/123"));
            assertTrue(filter.shouldMask("/v1/partner/auth/token"));
        }

        @Test
        @DisplayName("should not mask non-partner paths")
        void shouldNotMaskNonPartnerPaths() {
            assertFalse(filter.shouldMask("/api/v1/accounts"));
            assertFalse(filter.shouldMask("/api/v1/wallets/123"));
            assertFalse(filter.shouldMask("/health"));
        }

        @Test
        @DisplayName("should handle null path")
        void shouldHandleNullPath() {
            assertFalse(filter.shouldMask(null));
        }
    }

    @Nested
    @DisplayName("Field Masking")
    class FieldMasking {

        @Test
        @DisplayName("should remove blacklisted fields from flat object")
        void shouldRemoveBlacklistedFields() throws Exception {
            String json = "{\"data\": \"value\", \"stackTrace\": \"...\", \"traceId\": \"abc123\"}";
            JsonNode node = objectMapper.readTree(json);

            int removed = filter.maskFields(node);

            assertEquals(2, removed);
            assertFalse(node.has("stackTrace"));
            assertFalse(node.has("traceId"));
            assertTrue(node.has("data"));
        }

        @Test
        @DisplayName("should remove blacklisted fields from nested objects")
        void shouldRemoveFromNestedObjects() throws Exception {
            String json = """
                    {
                        "data": "value",
                        "error": {
                            "message": "Not found",
                            "internalErrorCode": "ERR_001",
                            "debugInfo": "stack details"
                        }
                    }
                    """;
            JsonNode node = objectMapper.readTree(json);

            int removed = filter.maskFields(node);

            assertEquals(2, removed);
            assertFalse(node.get("error").has("internalErrorCode"));
            assertFalse(node.get("error").has("debugInfo"));
            assertTrue(node.get("error").has("message"));
        }

        @Test
        @DisplayName("should remove from arrays of objects")
        void shouldRemoveFromArrays() throws Exception {
            String json = """
                    {
                        "items": [
                            {"name": "a", "serverHost": "host1"},
                            {"name": "b", "dbQuery": "SELECT *"}
                        ]
                    }
                    """;
            JsonNode node = objectMapper.readTree(json);

            int removed = filter.maskFields(node);

            assertEquals(2, removed);
            assertFalse(node.get("items").get(0).has("serverHost"));
            assertFalse(node.get("items").get(1).has("dbQuery"));
        }

        @Test
        @DisplayName("should not remove non-blacklisted fields")
        void shouldNotRemoveNonBlacklisted() throws Exception {
            String json = "{\"name\": \"test\", \"status\": \"active\", \"amount\": 100}";
            JsonNode node = objectMapper.readTree(json);

            int removed = filter.maskFields(node);

            assertEquals(0, removed);
            assertTrue(node.has("name"));
            assertTrue(node.has("status"));
            assertTrue(node.has("amount"));
        }

        @Test
        @DisplayName("should handle null node")
        void shouldHandleNull() {
            assertEquals(0, filter.maskFields(null));
        }

        @Test
        @DisplayName("should handle empty object")
        void shouldHandleEmptyObject() throws Exception {
            JsonNode node = objectMapper.readTree("{}");
            assertEquals(0, filter.maskFields(node));
        }
    }
}
