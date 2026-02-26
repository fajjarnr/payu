package id.payu.gateway.application.service;

import id.payu.gateway.config.GatewayConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DisplayName("RouteRegistry Unit Tests")
class RouteRegistryTest {

    private RouteRegistry registry;
    private GatewayConfig config;

    @BeforeEach
    void setUp() {
        config = Mockito.mock(GatewayConfig.class);

        // Create route configs
        GatewayConfig.RouteConfig accountRoute = mockRouteConfig(
                "account-service", "/api/v1/accounts", List.of("GET", "POST", "PUT", "DELETE"), true);
        GatewayConfig.RouteConfig partnerRoute = mockRouteConfig(
                "partner-service", "/partners", List.of("GET", "POST", "PUT", "DELETE"), true);
        GatewayConfig.RouteConfig walletRoute = mockRouteConfig(
                "wallet-service", "/api/v1/wallets", List.of("GET", "POST", "PUT"), true);
        GatewayConfig.RouteConfig disabledRoute = mockRouteConfig(
                "disabled-service", "/api/v1/disabled", List.of("GET"), false);
        GatewayConfig.RouteConfig publicContentRoute = mockRouteConfig(
                "cms-service", "/api/v1/public/contents", List.of("GET"), true);

        when(config.routes()).thenReturn(Map.of(
                "accounts", accountRoute,
                "partners", partnerRoute,
                "wallets", walletRoute,
                "disabled", disabledRoute,
                "public/contents", publicContentRoute
        ));

        registry = new RouteRegistry();
        try {
            var field = RouteRegistry.class.getDeclaredField("config");
            field.setAccessible(true);
            field.set(registry, config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        registry.loadRoutes();
    }

    private GatewayConfig.RouteConfig mockRouteConfig(String service, String targetPrefix,
                                                       List<String> methods, boolean enabled) {
        GatewayConfig.RouteConfig routeConfig = Mockito.mock(GatewayConfig.RouteConfig.class);
        when(routeConfig.service()).thenReturn(service);
        when(routeConfig.targetPrefix()).thenReturn(targetPrefix);
        when(routeConfig.methods()).thenReturn(methods);
        when(routeConfig.enabled()).thenReturn(enabled);
        return routeConfig;
    }

    @Nested
    @DisplayName("Route Resolution")
    class RouteResolution {

        @Test
        @DisplayName("should resolve simple path prefix")
        void shouldResolveSimplePrefix() {
            Optional<RouteRegistry.ResolvedRoute> resolved = registry.resolve("accounts/123");
            assertTrue(resolved.isPresent());
            assertEquals("account-service", resolved.get().serviceName());
            assertEquals("/api/v1/accounts/123", resolved.get().targetPath());
        }

        @Test
        @DisplayName("should resolve exact prefix match")
        void shouldResolveExactPrefix() {
            Optional<RouteRegistry.ResolvedRoute> resolved = registry.resolve("accounts");
            assertTrue(resolved.isPresent());
            assertEquals("account-service", resolved.get().serviceName());
            assertEquals("/api/v1/accounts", resolved.get().targetPath());
        }

        @Test
        @DisplayName("should resolve path with target prefix rewrite")
        void shouldResolveWithTargetRewrite() {
            Optional<RouteRegistry.ResolvedRoute> resolved = registry.resolve("partners/some/path");
            assertTrue(resolved.isPresent());
            assertEquals("partner-service", resolved.get().serviceName());
            assertEquals("/partners/some/path", resolved.get().targetPath());
        }

        @Test
        @DisplayName("should return empty for unknown path")
        void shouldReturnEmptyForUnknown() {
            Optional<RouteRegistry.ResolvedRoute> resolved = registry.resolve("unknown/path");
            assertTrue(resolved.isEmpty());
        }

        @Test
        @DisplayName("should return empty for disabled route")
        void shouldReturnEmptyForDisabled() {
            Optional<RouteRegistry.ResolvedRoute> resolved = registry.resolve("disabled/path");
            assertTrue(resolved.isEmpty());
        }

        @Test
        @DisplayName("should return empty for null path")
        void shouldReturnEmptyForNull() {
            assertTrue(registry.resolve(null).isEmpty());
        }

        @Test
        @DisplayName("should return empty for blank path")
        void shouldReturnEmptyForBlank() {
            assertTrue(registry.resolve("").isEmpty());
        }

        @Test
        @DisplayName("should resolve longest prefix match")
        void shouldResolveLongestPrefixMatch() {
            Optional<RouteRegistry.ResolvedRoute> resolved = registry.resolve("public/contents/banners");
            assertTrue(resolved.isPresent());
            assertEquals("cms-service", resolved.get().serviceName());
            assertEquals("/api/v1/public/contents/banners", resolved.get().targetPath());
        }
    }

    @Nested
    @DisplayName("Route Management")
    class RouteManagement {

        @Test
        @DisplayName("should return all routes")
        void shouldReturnAllRoutes() {
            Map<String, RouteRegistry.RouteDefinition> allRoutes = registry.getAllRoutes();
            assertEquals(5, allRoutes.size());
            assertTrue(allRoutes.containsKey("accounts"));
            assertTrue(allRoutes.containsKey("partners"));
        }

        @Test
        @DisplayName("should return route count")
        void shouldReturnRouteCount() {
            assertEquals(5, registry.getRouteCount());
        }
    }

    @Nested
    @DisplayName("Default Routes")
    class DefaultRoutes {

        @Test
        @DisplayName("should load defaults when config is empty")
        void shouldLoadDefaultsWhenEmpty() {
            when(config.routes()).thenReturn(Collections.emptyMap());
            registry.loadRoutes();

            // Should have default routes
            assertTrue(registry.getRouteCount() > 10, "Should have many default routes");
            assertTrue(registry.resolve("accounts").isPresent());
            assertTrue(registry.resolve("wallets").isPresent());
            assertTrue(registry.resolve("transactions").isPresent());
        }
    }
}
