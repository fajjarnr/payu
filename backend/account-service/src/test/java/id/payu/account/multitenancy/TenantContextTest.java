package id.payu.account.multitenancy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TenantContextTest {

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void testSetAndGetTenantId() {
        TenantContext.setTenantId("tenant-123");
        assertEquals("tenant-123", TenantContext.getTenantId());
    }

    @Test
    void testSetNullTenantId() {
        TenantContext.setTenantId(null);
        assertEquals("default", TenantContext.getTenantId());
    }

    @Test
    void testSetBlankTenantId() {
        TenantContext.setTenantId("   ");
        assertEquals("default", TenantContext.getTenantId());
    }

    @Test
    void testDefaultTenantId() {
        assertEquals("default", TenantContext.getTenantId());
    }

    @Test
    void testClearTenantId() {
        TenantContext.setTenantId("tenant-123");
        TenantContext.clear();
        assertEquals("default", TenantContext.getTenantId());
    }

    @Test
    void testMultipleTenantsInSequence() {
        TenantContext.setTenantId("tenant-1");
        assertEquals("tenant-1", TenantContext.getTenantId());

        TenantContext.setTenantId("tenant-2");
        assertEquals("tenant-2", TenantContext.getTenantId());
    }
}
