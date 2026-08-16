package id.payu.gateway.application.service;

import id.payu.gateway.config.GatewayConfig;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic route registry that maps URL path prefixes to backend services.
 * <p>
 * Replaces the ~70 hardcoded JAX-RS endpoints with a config-driven route table (IMP-007).
 * Routes are loaded from gateway.routes configuration at startup and can be reloaded.
 * <p>
 * Route resolution:
 * 1. Extract the path prefix from the request path (e.g., "accounts" from "/api/v1/accounts/123")
 * 2. Look up the route definition from the registry
 * 3. Build the backend target path using the route's target-prefix
 */
@ApplicationScoped
public class RouteRegistry {

    @Inject
    GatewayConfig config;

    /**
     * Map of gateway path prefix → RouteDefinition.
     * Key is the first path segment after /api/v1/ (e.g., "accounts", "wallets").
     */
    private final ConcurrentHashMap<String, RouteDefinition> routes = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        loadRoutes();
        Log.infof("RouteRegistry initialized with %d routes", routes.size());
    }

    /**
     * Load routes from configuration.
     */
    public void loadRoutes() {
        routes.clear();

        Map<String, GatewayConfig.RouteConfig> configRoutes = config.routes();
        if (configRoutes == null || configRoutes.isEmpty()) {
            Log.warn("No routes configured in gateway.routes — using defaults");
            loadDefaultRoutes();
            return;
        }

        configRoutes.forEach((prefix, routeConfig) -> {
            RouteDefinition def = new RouteDefinition(
                    prefix,
                    routeConfig.service(),
                    routeConfig.targetPrefix(),
                    routeConfig.methods(),
                    routeConfig.enabled()
            );
            routes.put(prefix, def);
            Log.debugf("Registered route: %s → %s (target: %s, methods: %s)",
                    prefix, routeConfig.service(), routeConfig.targetPrefix(), routeConfig.methods());
        });
    }

    /**
     * Resolve a request path to a route definition.
     *
     * @param pathAfterApiV1 the path after /api/v1/ (e.g., "accounts/123" or "v1/partner/auth/token")
     * @return Optional<ResolvedRoute> if a matching route is found
     */
    public Optional<ResolvedRoute> resolve(String pathAfterApiV1) {
        if (pathAfterApiV1 == null || pathAfterApiV1.isBlank()) {
            return Optional.empty();
        }

        // Try longest prefix match first
        String bestMatch = null;
        RouteDefinition bestDef = null;

        for (Map.Entry<String, RouteDefinition> entry : routes.entrySet()) {
            String prefix = entry.getKey();
            if (pathAfterApiV1.equals(prefix) || pathAfterApiV1.startsWith(prefix + "/")) {
                if (bestMatch == null || prefix.length() > bestMatch.length()) {
                    bestMatch = prefix;
                    bestDef = entry.getValue();
                }
            }
        }

        if (bestDef == null || !bestDef.enabled()) {
            return Optional.empty();
        }

        // Build backend target path
        String subPath = pathAfterApiV1.substring(bestMatch.length());
        String targetPath = bestDef.targetPrefix() + subPath;

        return Optional.of(new ResolvedRoute(bestDef.serviceName(), targetPath, bestDef));
    }

    /**
     * Get all registered routes (for admin/health endpoints).
     */
    public Map<String, RouteDefinition> getAllRoutes() {
        return Collections.unmodifiableMap(routes);
    }

    /**
     * Get route count.
     */
    public int getRouteCount() {
        return routes.size();
    }

    /**
     * Load default routes matching the hardcoded endpoints that were in ApiGatewayResource.
     * This ensures backward compatibility even without explicit YAML config.
     */
    private void loadDefaultRoutes() {
        // Account Service
        registerDefault("accounts", "account-service", "/api/v1/accounts");

        // Wallet Service
        registerDefault("wallets", "wallet-service", "/api/v1/wallets");
        registerDefault("cards", "wallet-service", "/api/v1/cards");

        // Transaction Service
        registerDefault("transactions", "transaction-service", "/api/v1/transactions");
        registerDefault("disbursements", "transaction-service", "/api/v1/disbursements");
        registerDefault("smart-routing", "transaction-service", "/api/v1/transfers/routes");
        registerDefault("transfers/routes", "transaction-service", "/api/v1/transfers/routes");
        registerDefault("split-bills", "transaction-service", "/api/v1/split-bills");
        registerDefault("qris", "transaction-service", "/api/v1/qris");
        registerDefault("payments/va", "transaction-service", "/api/v1/payments/va");

        // Wallet Service extras
        registerDefault("savings-goals", "wallet-service", "/api/v1/wallets");
        registerDefault("escrow", "wallet-service", "/api/v1/escrow");
        registerDefault("settlements", "wallet-service", "/api/v1/settlements");

        // Billing Service
        registerDefault("billers", "billing-service", "/api/v1/billers");
        registerDefault("payments", "billing-service", "/api/v1/payments");

        // Notification Service
        registerDefault("notifications", "notification-service", "/api/v1/notifications");

        // Auth Service
        registerDefault("auth", "auth-service", "/api/v1/auth");

        // Partner Service
        registerDefault("partners", "partner-service", "/partners");
        registerDefault("v1/partner", "partner-service", "/v1/partner");
        registerDefault("v1.0", "partner-service", "/v1.0");

        // Promotion Service
        registerDefault("promotions", "promotion-service", "/api/v1/promotions");
        registerDefault("cashbacks", "promotion-service", "/api/v1/cashbacks");
        registerDefault("loyalty-points", "promotion-service", "/api/v1/loyalty-points");
        registerDefault("rewards", "promotion-service", "/api/v1/rewards");
        registerDefault("referrals", "promotion-service", "/api/v1/referrals");

        // Lending Service
        registerDefault("lending", "lending-service", "/api/v1/lending");

        // Investment Service
        registerDefault("investments", "investment-service", "/api/v1/investments");

        // Compliance Service
        registerDefault("compliance", "compliance-service", "/api/v1/compliance");

        // Backoffice Service
        registerDefault("backoffice", "backoffice-service", "/api/v1/backoffice");

        // Support Service
        registerDefault("support", "support-service", "/api/v1/support");

        // CMS Service
        registerDefault("contents", "cms-service", "/api/v1/contents");
        registerDefault("public/contents", "cms-service", "/api/v1/public/contents");

        // Billing Service - TopUp via alternate prefix
        registerDefault("billing/topup", "billing-service", "/api/v1/topup");

        // CMS Service (alternate /cms prefix)
        registerDefault("cms", "cms-service", "/api/v1/contents");

        // Product Catalog Service
        registerDefault("products", "product-catalog-service", "/products");

        // Integration Service
        registerDefault("integration", "integration-service", "/api/v1/integration");

        // TopUp - add explicit topup route
        registerDefault("topup", "billing-service", "/api/v1/topup");
    }

    private void registerDefault(String prefix, String service, String targetPrefix) {
        routes.put(prefix, new RouteDefinition(
                prefix, service, targetPrefix,
                List.of("GET", "POST", "PUT", "DELETE", "PATCH"),
                true
        ));
    }

    // ==================== Inner Classes ====================

    /**
     * A registered route definition.
     */
    public record RouteDefinition(
            String prefix,
            String serviceName,
            String targetPrefix,
            List<String> methods,
            boolean enabled
    ) {}

    /**
     * A resolved route ready for proxying.
     */
    public record ResolvedRoute(
            String serviceName,
            String targetPath,
            RouteDefinition definition
    ) {}
}
