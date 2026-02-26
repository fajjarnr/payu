package id.payu.gateway.adapter.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import id.payu.gateway.config.GatewayConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DisplayName("RequestValidationFilter Unit Tests")
class RequestValidationFilterTest {

    private RequestValidationFilter filter;
    private GatewayConfig config;
    private GatewayConfig.ValidationConfig validationConfig;

    @BeforeEach
    void setUp() {
        config = Mockito.mock(GatewayConfig.class);
        validationConfig = Mockito.mock(GatewayConfig.ValidationConfig.class);
        when(config.validation()).thenReturn(validationConfig);
        when(validationConfig.enabled()).thenReturn(true);
        when(validationConfig.schemaValidation()).thenReturn(true);
        when(validationConfig.maxRequestSize()).thenReturn(10485760L);
        when(validationConfig.strictMode()).thenReturn(false);

        filter = new RequestValidationFilter();
        try {
            var configField = RequestValidationFilter.class.getDeclaredField("config");
            configField.setAccessible(true);
            configField.set(filter, config);

            var omField = RequestValidationFilter.class.getDeclaredField("objectMapper");
            omField.setAccessible(true);
            omField.set(filter, new ObjectMapper());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        filter.init();
    }

    @Nested
    @DisplayName("Schema Resolution")
    class SchemaResolution {

        @Test
        @DisplayName("should resolve auth login schema")
        void shouldResolveAuthLoginSchema() {
            JsonSchema schema = filter.getSchemaForPath("/api/v1/auth/login");
            assertNotNull(schema, "Auth login schema should be loaded from classpath");
        }

        @Test
        @DisplayName("should resolve auth register schema")
        void shouldResolveAuthRegisterSchema() {
            JsonSchema schema = filter.getSchemaForPath("/api/v1/auth/register");
            assertNotNull(schema, "Auth register schema should be loaded");
        }

        @Test
        @DisplayName("should resolve accounts create schema")
        void shouldResolveAccountsCreateSchema() {
            JsonSchema schema = filter.getSchemaForPath("/api/v1/accounts");
            assertNotNull(schema, "Accounts create schema should be loaded");
        }

        @Test
        @DisplayName("should resolve transactions transfer schema")
        void shouldResolveTransactionsTransferSchema() {
            JsonSchema schema = filter.getSchemaForPath("/api/v1/transactions/transfer");
            assertNotNull(schema, "Transactions transfer schema should be loaded");
        }

        @Test
        @DisplayName("should resolve payments create schema")
        void shouldResolvePaymentsCreateSchema() {
            JsonSchema schema = filter.getSchemaForPath("/api/v1/payments");
            assertNotNull(schema, "Payments create schema should be loaded");
        }

        @Test
        @DisplayName("should resolve partners create schema")
        void shouldResolvePartnersCreateSchema() {
            JsonSchema schema = filter.getSchemaForPath("/api/v1/partners");
            assertNotNull(schema, "Partners create schema should be loaded");
        }

        @Test
        @DisplayName("should return null for unmapped path")
        void shouldReturnNullForUnmappedPath() {
            JsonSchema schema = filter.getSchemaForPath("/api/v1/wallets");
            assertNull(schema, "Should return null for path without schema");
        }

        @Test
        @DisplayName("should resolve longest prefix match")
        void shouldResolveLongestPrefixMatch() {
            // /api/v1/transactions/transfer should match transfer schema, not generic transactions
            JsonSchema transferSchema = filter.getSchemaForPath("/api/v1/transactions/transfer");
            JsonSchema genericSchema = filter.getSchemaForPath("/api/v1/transactions");

            assertNotNull(transferSchema);
            assertNotNull(genericSchema);
            // They should be different schemas
            assertNotEquals(transferSchema, genericSchema);
        }
    }

    @Nested
    @DisplayName("Schema Validation")
    class SchemaValidation {

        @Test
        @DisplayName("should validate valid auth login request")
        void shouldValidateValidAuthLogin() {
            JsonSchema schema = filter.getSchemaForPath("/api/v1/auth/login");
            assertNotNull(schema);

            var node = new ObjectMapper().createObjectNode()
                    .put("username", "testuser")
                    .put("password", "securepass123");

            var errors = schema.validate(node);
            assertTrue(errors.isEmpty(), "Valid login should pass validation: " + errors);
        }

        @Test
        @DisplayName("should reject auth login without required fields")
        void shouldRejectLoginWithoutRequired() {
            JsonSchema schema = filter.getSchemaForPath("/api/v1/auth/login");
            assertNotNull(schema);

            var node = new ObjectMapper().createObjectNode()
                    .put("username", "testuser");
            // Missing password

            var errors = schema.validate(node);
            assertFalse(errors.isEmpty(), "Missing password should fail validation");
        }

        @Test
        @DisplayName("should reject transfer with zero amount")
        void shouldRejectTransferWithZeroAmount() {
            JsonSchema schema = filter.getSchemaForPath("/api/v1/transactions/transfer");
            assertNotNull(schema);

            var node = new ObjectMapper().createObjectNode()
                    .put("sourceAccountId", "acc-1")
                    .put("destinationAccountId", "acc-2")
                    .put("amount", 0); // Must be > 0

            var errors = schema.validate(node);
            assertFalse(errors.isEmpty(), "Zero amount should fail validation");
        }

        @Test
        @DisplayName("should validate valid transfer request")
        void shouldValidateValidTransfer() {
            JsonSchema schema = filter.getSchemaForPath("/api/v1/transactions/transfer");
            assertNotNull(schema);

            var node = new ObjectMapper().createObjectNode()
                    .put("sourceAccountId", "acc-1")
                    .put("destinationAccountId", "acc-2")
                    .put("amount", 100000)
                    .put("currency", "IDR");

            var errors = schema.validate(node);
            assertTrue(errors.isEmpty(), "Valid transfer should pass: " + errors);
        }
    }
}
