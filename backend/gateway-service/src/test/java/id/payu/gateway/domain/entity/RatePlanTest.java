package id.payu.gateway.domain.entity;

import id.payu.gateway.domain.vo.RateLimit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RatePlan domain entity.
 */
class RatePlanTest {

    @Test
    void shouldCreateRatePlanWithDefaultLimit() {
        RatePlan plan = new RatePlan("plan-1", "Default Plan", "Standard limits",
            RateLimit.of(60, 1000, 10000));

        assertEquals("plan-1", plan.getId());
        assertEquals("Default Plan", plan.getName());
        assertTrue(plan.isActive());
        assertEquals(60, plan.getDefaultLimit().requestsPerMinute());
    }

    @Test
    void shouldAddEndpointOverride() {
        RatePlan plan = new RatePlan("plan-1", "Default Plan", "Standard limits",
            RateLimit.of(60, 1000, 10000));

        plan.addEndpointOverride("/api/v1/transfer", RateLimit.of(10, 100, 1000));

        assertTrue(plan.hasEndpointOverride("/api/v1/transfer"));
        assertEquals(10, plan.getEffectiveLimit("/api/v1/transfer").requestsPerMinute());
    }

    @Test
    void shouldReturnDefaultLimitForNonOverriddenEndpoint() {
        RatePlan plan = new RatePlan("plan-1", "Default Plan", "Standard limits",
            RateLimit.of(60, 1000, 10000));

        plan.addEndpointOverride("/api/v1/transfer", RateLimit.of(10, 100, 1000));

        RateLimit limit = plan.getEffectiveLimit("/api/v1/accounts");
        assertEquals(60, limit.requestsPerMinute());
    }

    @Test
    void shouldMatchWildcardPattern() {
        RatePlan plan = new RatePlan("plan-1", "Default Plan", "Standard limits",
            RateLimit.of(60, 1000, 10000));

        plan.addEndpointOverride("/api/v1/accounts/*", RateLimit.of(30, 500, 5000));

        RateLimit limit = plan.getEffectiveLimit("/api/v1/accounts/123");
        assertEquals(30, limit.requestsPerMinute());
    }

    @Test
    void shouldActivateAndDeactivate() {
        RatePlan plan = new RatePlan("plan-1", "Default Plan", "Standard limits",
            RateLimit.of(60, 1000, 10000));

        assertTrue(plan.isActive());

        plan.deactivate();
        assertFalse(plan.isActive());

        plan.activate();
        assertTrue(plan.isActive());
    }

    @Test
    void shouldUpdateName() {
        RatePlan plan = new RatePlan("plan-1", "Default Plan", "Standard limits",
            RateLimit.of(60, 1000, 10000));

        plan.updateName("Updated Plan");

        assertEquals("Updated Plan", plan.getName());
        assertNotNull(plan.getUpdatedAt());
    }

    @Test
    void shouldUpdateDefaultLimit() {
        RatePlan plan = new RatePlan("plan-1", "Default Plan", "Standard limits",
            RateLimit.of(60, 1000, 10000));

        plan.updateDefaultLimit(RateLimit.of(120, 2000, 20000));

        assertEquals(120, plan.getDefaultLimit().requestsPerMinute());
    }

    @Test
    void shouldRemoveEndpointOverride() {
        RatePlan plan = new RatePlan("plan-1", "Default Plan", "Standard limits",
            RateLimit.of(60, 1000, 10000));

        plan.addEndpointOverride("/api/v1/transfer", RateLimit.of(10, 100, 1000));
        assertTrue(plan.hasEndpointOverride("/api/v1/transfer"));

        plan.removeEndpointOverride("/api/v1/transfer");
        assertFalse(plan.hasEndpointOverride("/api/v1/transfer"));
    }

    @Test
    void shouldThrowExceptionForNullId() {
        assertThrows(NullPointerException.class, () ->
            new RatePlan(null, "Name", "Description", RateLimit.defaultLimits()));
    }

    @Test
    void shouldThrowExceptionForNullName() {
        assertThrows(NullPointerException.class, () ->
            new RatePlan("id", null, "Description", RateLimit.defaultLimits()));
    }

    @Test
    void shouldThrowExceptionForNullLimit() {
        assertThrows(NullPointerException.class, () ->
            new RatePlan("id", "Name", "Description", null));
    }
}
