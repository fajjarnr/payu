package id.payu.partner.multitenancy;

import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.adapter.persistence.entity.WebhookSubscriptionEntity;
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
        @DisplayName("should auto-set tenantId on PartnerEntity creation")
        void shouldAutoSetTenantIdOnPartnerCreate() {
            TenantContext.setTenantId("tokobapak");

            PartnerEntity partner = new PartnerEntity();
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

            PartnerEntity partner = new PartnerEntity();
            partner.setTenantId("existing-tenant");

            listener.setTenantOnCreate(partner);

            assertEquals("existing-tenant", partner.getTenantId());
        }

        @Test
        @DisplayName("should auto-set tenantId on WebhookSubscriptionEntity creation")
        void shouldAutoSetTenantIdOnWebhookCreate() {
            TenantContext.setTenantId("nobar");

            PartnerEntity partner = new PartnerEntity();
            partner.setId(1L);
            partner.setActive(true);

            WebhookSubscriptionEntity sub = new WebhookSubscriptionEntity(
                    partner, "https://example.com/wh", "*", "secret");

            listener.setTenantOnCreate(sub);

            assertEquals("nobar", sub.getTenantId());
        }

        @Test
        @DisplayName("should reject cross-tenant update")
        void shouldRejectCrossTenantUpdate() {
            TenantContext.setTenantId("tenant-a");

            PartnerEntity partner = new PartnerEntity();
            partner.setTenantId("tenant-b");

            assertThrows(SecurityException.class,
                    () -> listener.validateTenantOnUpdate(partner));
        }

        @Test
        @DisplayName("should allow same-tenant update")
        void shouldAllowSameTenantUpdate() {
            TenantContext.setTenantId("tenant-a");

            PartnerEntity partner = new PartnerEntity();
            partner.setTenantId("tenant-a");

            assertDoesNotThrow(() -> listener.validateTenantOnUpdate(partner));
        }

        @Test
        @DisplayName("should skip validation when tenant context not set")
        void shouldSkipValidationWhenNotSet() {
            // TenantContext defaults to "default", isSet() returns false
            PartnerEntity partner = new PartnerEntity();
            partner.setTenantId("any-tenant");

            assertDoesNotThrow(() -> listener.validateTenantOnUpdate(partner));
        }
    }
}
