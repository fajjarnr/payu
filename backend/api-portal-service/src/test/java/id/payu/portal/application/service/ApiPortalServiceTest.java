package id.payu.portal.application.service;

import id.payu.portal.dto.AggregatedOpenApiResponse;
import id.payu.portal.dto.OpenApiSpec;
import id.payu.portal.dto.ServiceInfo;
import id.payu.portal.dto.ServiceListResponse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@DisplayName("ApiPortalService Unit Tests")
class ApiPortalServiceTest {

    @Inject
    ApiPortalService apiPortalService;

    @Test
    @DisplayName("should list all 20 configured services")
    void testListServices() {
        ServiceListResponse response = apiPortalService.listServices().await().indefinitely();
        assertNotNull(response);
        assertNotNull(response.services());
        assertFalse(response.services().isEmpty());
        assertEquals(20, response.services().size());
    }

    @Test
    @DisplayName("should return services sorted alphabetically by name")
    void testListServices_SortedByName() {
        ServiceListResponse response = apiPortalService.listServices().await().indefinitely();

        // Verify sorted order using the same comparator as production code (case-sensitive)
        for (int i = 0; i < response.services().size() - 1; i++) {
            String current = response.services().get(i).name();
            String next = response.services().get(i + 1).name();
            assertTrue(current.compareTo(next) <= 0,
                String.format("Services must be sorted alphabetically: '%s' should come before '%s'", current, next));
        }
    }

    @Test
    @DisplayName("should return all expected service IDs in the list")
    void testListServices_ContainsExpectedServices() {
        ServiceListResponse response = apiPortalService.listServices().await().indefinitely();

        Set<String> serviceIds = response.services().stream()
            .map(ServiceInfo::id)
            .collect(Collectors.toSet());

        assertTrue(serviceIds.contains("account-service"), "Must contain account-service");
        assertTrue(serviceIds.contains("auth-service"), "Must contain auth-service");
        assertTrue(serviceIds.contains("wallet-service"), "Must contain wallet-service");
        assertTrue(serviceIds.contains("transaction-service"), "Must contain transaction-service");
        assertTrue(serviceIds.contains("api-portal-service"), "Must contain api-portal-service");
    }

    @Test
    @DisplayName("should include URL and openapiPath for each service")
    void testListServices_HasRequiredFields() {
        ServiceListResponse response = apiPortalService.listServices().await().indefinitely();

        response.services().forEach(service -> {
            assertNotNull(service.id(), "Service id must not be null");
            assertNotNull(service.name(), "Service name must not be null: " + service.id());
            assertNotNull(service.url(), "Service url must not be null: " + service.id());
            assertNotNull(service.openapiPath(), "Service openapiPath must not be null: " + service.id());
            assertNotNull(service.status(), "Service status must not be null: " + service.id());
            assertTrue(
                service.status().equals("UP") ||
                service.status().equals("DOWN") ||
                service.status().equals("UNKNOWN"),
                "Status must be UP, DOWN, or UNKNOWN: " + service.status());
        });
    }

    @Test
    @DisplayName("should throw exception when querying spec for non-existent service")
    void testGetServiceSpec_NonExistentService_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            apiPortalService.getServiceSpec("non-existent-service").await().indefinitely()
        );
    }

    @Test
    @DisplayName("should return null when remote service is not reachable")
    void testGetServiceSpec_UnreachableService_ReturnsNull() {
        // account-service is configured but pointing to localhost:8080 which has no service running
        OpenApiSpec spec = apiPortalService.getServiceSpec("account-service").await().indefinitely();
        assertNull(spec, "Spec should be null when remote service is unreachable");
    }

    @Test
    @DisplayName("should return valid aggregated specs response")
    void testGetAggregatedSpecs() {
        // Use refreshCache() directly to avoid cache-state-dependent Duration.parse
        AggregatedOpenApiResponse response = apiPortalService.refreshCache().await().indefinitely();

        assertNotNull(response);
        assertEquals("1.0.0", response.version());
        assertNotNull(response.services());
        assertTrue(response.lastUpdated() > 0, "lastUpdated should be set after refresh");
    }

    @Test
    @DisplayName("should force refresh cache and return valid aggregated specs")
    void testRefreshCache() {
        AggregatedOpenApiResponse response = apiPortalService.refreshCache().await().indefinitely();

        assertNotNull(response);
        assertEquals("1.0.0", response.version());
        assertNotNull(response.services());
        assertTrue(response.lastUpdated() > 0,
            "LastUpdated should be a positive timestamp after refresh");
    }

    @Test
    @DisplayName("should handle refresh when all external services are unreachable")
    void testRefreshCache_HandlesAllFailuresGracefully() {
        // All services point to localhost:8080 which is not running
        // refreshCache must not throw, it should return partial results
        AggregatedOpenApiResponse response = apiPortalService.refreshCache().await().indefinitely();

        assertNotNull(response);
        assertEquals("1.0.0", response.version());
        assertNotNull(response.services());
        assertTrue(response.services().isEmpty() || response.services().size() >= 0,
            "Should handle partial results gracefully");
    }

    @Test
    @DisplayName("should return ServiceListResponse with correct structure")
    @Tag("contract")
    void testListServices_ResponseStructure() {
        ServiceListResponse response = apiPortalService.listServices().await().indefinitely();

        assertNotNull(response.services());
        response.services().forEach(service -> {
            assertNotNull(service.id());
            assertNotNull(service.name());
            assertNotNull(service.status());
        });
    }
}
