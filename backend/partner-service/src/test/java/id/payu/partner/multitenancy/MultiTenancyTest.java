package id.payu.partner.multitenancy;

import id.payu.partner.domain.Partner;
import id.payu.partner.domain.WebhookSubscription;
import id.payu.security.multitenancy.TenantContext;
import id.payu.security.multitenancy.TenantEntityListener;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for multi-tenancy infrastructure applied to partner-service entities.
 */
class MultiTenancyTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Nested
    @DisplayName("TenantContext")
    class TenantContextTests {

        @Test
        @DisplayName("should return 'default' when not set")
        void shouldReturnDefaultWhenNotSet() {
            assertEquals("default", TenantContext.getTenantId());
            assertFalse(TenantContext.isSet());
        }

        @Test
        @DisplayName("should store and retrieve tenant ID")
        void shouldStoreAndRetrieveTenantId() {
            TenantContext.setTenantId("partner-tokobapak");
            assertEquals("partner-tokobapak", TenantContext.getTenantId());
            assertTrue(TenantContext.isSet());
        }

        @Test
        @DisplayName("should fallback to default for null/blank")
        void shouldFallbackToDefaultForNullOrBlank() {
            TenantContext.setTenantId(null);
            assertEquals("default", TenantContext.getTenantId());

            TenantContext.setTenantId("  ");
            assertEquals("default", TenantContext.getTenantId());
        }

        @Test
        @DisplayName("should clear tenant context")
        void shouldClearTenantContext() {
            TenantContext.setTenantId("test-tenant");
            assertTrue(TenantContext.isSet());

            TenantContext.clear();
            assertFalse(TenantContext.isSet());
            assertEquals("default", TenantContext.getTenantId());
        }

        @Test
        @DisplayName("should trim whitespace from tenant ID")
        void shouldTrimWhitespace() {
            TenantContext.setTenantId("  partner-123  ");
            assertEquals("partner-123", TenantContext.getTenantId());
        }
    }

    @Nested
    @DisplayName("TenantEntityListener")
    class EntityListenerTests {

        private final TenantEntityListener listener = new TenantEntityListener();

        @Test
        @DisplayName("should auto-set tenantId on Partner creation")
        void shouldAutoSetTenantIdOnPartnerCreate() {
            TenantContext.setTenantId("tokobapak");

            Partner partner = new Partner();
            partner.setName("TokoBapak");
            partner.setType("MERCHANT");
            partner.setEmail("test@example.com");

            listener.setTenantOnCreate(partner);

            assertEquals("tokobapak", partner.getTenantId());
        }

        @Test
        @DisplayName("should not overwrite existing tenantId on create")
        void shouldNotOverwriteExistingTenantId() {
            TenantContext.setTenantId("new-tenant");

            Partner partner = new Partner();
            partner.setTenantId("existing-tenant");

            listener.setTenantOnCreate(partner);

            assertEquals("existing-tenant", partner.getTenantId());
        }

        @Test
        @DisplayName("should auto-set tenantId on WebhookSubscription creation")
        void shouldAutoSetTenantIdOnWebhookCreate() {
            TenantContext.setTenantId("nobar");

            Partner partner = new Partner();
            partner.setId(1L);
            partner.setActive(true);

            WebhookSubscription sub = new WebhookSubscription(
                    partner, "https://example.com/wh", "*", "secret");

            listener.setTenantOnCreate(sub);

            assertEquals("nobar", sub.getTenantId());
        }

        @Test
        @DisplayName("should reject cross-tenant update")
        void shouldRejectCrossTenantUpdate() {
            TenantContext.setTenantId("tenant-a");

            Partner partner = new Partner();
            partner.setTenantId("tenant-b");

            assertThrows(SecurityException.class,
                    () -> listener.validateTenantOnUpdate(partner));
        }

        @Test
        @DisplayName("should allow same-tenant update")
        void shouldAllowSameTenantUpdate() {
            TenantContext.setTenantId("tenant-a");

            Partner partner = new Partner();
            partner.setTenantId("tenant-a");

            assertDoesNotThrow(() -> listener.validateTenantOnUpdate(partner));
        }

        @Test
        @DisplayName("should skip validation when tenant context not set")
        void shouldSkipValidationWhenNotSet() {
            // TenantContext defaults to "default", isSet() returns false
            Partner partner = new Partner();
            partner.setTenantId("any-tenant");

            assertDoesNotThrow(() -> listener.validateTenantOnUpdate(partner));
        }
    }
}
