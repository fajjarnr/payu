package id.payu.gateway.domain.entity;

import id.payu.gateway.domain.vo.RateLimit;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Domain entity representing a rate plan for API partners.
 * <p>
 * A rate plan defines the rate limits that apply to a partner or group of partners.
 * It supports both default limits and per-endpoint overrides.
 * <p>
 * This is an aggregate root in the Rate Limiting bounded context.
 */
public class RatePlan {

    private final String id;
    private String name;
    private String description;
    private RateLimit defaultLimit;
    private final Map<String, RateLimit> endpointOverrides;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    public RatePlan(String id, String name, String description, RateLimit defaultLimit) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.description = description;
        this.defaultLimit = Objects.requireNonNull(defaultLimit, "Default limit cannot be null");
        this.endpointOverrides = new HashMap<>();
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // Domain behavior

    public void activate() {
        this.active = true;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public void updateName(String newName) {
        this.name = Objects.requireNonNull(newName, "Name cannot be null");
        this.updatedAt = Instant.now();
    }

    public void updateDescription(String newDescription) {
        this.description = newDescription;
        this.updatedAt = Instant.now();
    }

    public void updateDefaultLimit(RateLimit newLimit) {
        this.defaultLimit = Objects.requireNonNull(newLimit, "Limit cannot be null");
        this.updatedAt = Instant.now();
    }

    public void addEndpointOverride(String endpointPattern, RateLimit limit) {
        Objects.requireNonNull(endpointPattern, "Endpoint pattern cannot be null");
        Objects.requireNonNull(limit, "Limit cannot be null");
        this.endpointOverrides.put(endpointPattern, limit);
        this.updatedAt = Instant.now();
    }

    public void removeEndpointOverride(String endpointPattern) {
        this.endpointOverrides.remove(endpointPattern);
        this.updatedAt = Instant.now();
    }

    /**
     * Get the effective rate limit for a given endpoint.
     * Returns endpoint override if exists, otherwise returns default limit.
     */
    public RateLimit getEffectiveLimit(String endpoint) {
        Objects.requireNonNull(endpoint, "Endpoint cannot be null");

        // Check for exact match first
        if (endpointOverrides.containsKey(endpoint)) {
            return endpointOverrides.get(endpoint);
        }

        // Check for pattern matches (e.g., "/api/v1/accounts/*" matches "/api/v1/accounts/123")
        for (Map.Entry<String, RateLimit> entry : endpointOverrides.entrySet()) {
            String pattern = entry.getKey();
            if (matchesPattern(endpoint, pattern)) {
                return entry.getValue();
            }
        }

        return defaultLimit;
    }

    private boolean matchesPattern(String endpoint, String pattern) {
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return endpoint.startsWith(prefix);
        }
        return endpoint.equals(pattern);
    }

    public boolean hasEndpointOverride(String endpoint) {
        return endpointOverrides.containsKey(endpoint);
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public RateLimit getDefaultLimit() {
        return defaultLimit;
    }

    public Map<String, RateLimit> getEndpointOverrides() {
        return Collections.unmodifiableMap(endpointOverrides);
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RatePlan ratePlan = (RatePlan) o;
        return Objects.equals(id, ratePlan.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
