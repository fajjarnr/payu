package id.payu.gateway.service;

import id.payu.gateway.config.GatewayConfig;
import io.quarkus.logging.Log;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;

/**
 * Service to manage API key rotation.
 * Automatically generates and rotates API keys on a schedule.
 */
@ApplicationScoped
public class ApiKeyRotationService {

    private static final String API_KEY_PREFIX = "apikey:";
    private static final String API_KEY_METADATA_PREFIX = "apikey:meta:";
    private static final int KEY_LENGTH = 32; // 256 bits
    private final SecureRandom random = new SecureRandom();

    @Inject
    GatewayConfig config;

    @Inject
    ReactiveRedisDataSource redis;

    private boolean enabled;

    @PostConstruct
    void init() {
        this.enabled = config.apiKeys().enabled() && config.apiKeys().rotation().enabled();
        Log.infof("API Key Rotation service initialized (enabled: %s)", enabled);
    }

    /**
     * Generate a new API key.
     */
    public String generateApiKey(String userId) {
        byte[] keyBytes = new byte[KEY_LENGTH];
        random.nextBytes(keyBytes);
        String apiKey = Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);

        // Store in Redis
        String redisKey = API_KEY_PREFIX + apiKey;
        Instant expiresAt = Instant.now().plus(config.apiKeys().rotation().autoRotateDays(), ChronoUnit.DAYS);

        redis.value(String.class)
            .setex(redisKey, config.apiKeys().rotation().autoRotateDays() * 86400, userId)
            .subscribe()
            .with(
                unused -> {
                    Log.infof("Generated new API key for user %s, expires at %s", userId, expiresAt);
                    // Store metadata
                    storeMetadata(apiKey, userId, expiresAt);
                },
                failure -> Log.errorf(failure, "Failed to store API key for user %s", userId)
            );

        return apiKey;
    }

    /**
     * Validate an API key.
     */
    public Uni<String> validateApiKey(String apiKey) {
        if (!enabled) {
            return Uni.createFrom().item((String) null);
        }

        String redisKey = API_KEY_PREFIX + apiKey;

        return redis.value(String.class).get(redisKey)
            .map(userId -> {
                Log.debugf("API key validated for user: %s", userId);
                return userId;
            })
            .onFailure().recoverWithUni(throwable -> {
                Log.warnf(throwable, "Failed to validate API key");
                return Uni.createFrom().item((String) null);
            });
    }

    /**
     * Rotate an API key (generate new one, invalidate old).
     */
    public Uni<String> rotateApiKey(String oldApiKey) {
        // Get user ID from old key
        String redisKey = API_KEY_PREFIX + oldApiKey;

        return redis.value(String.class).get(redisKey)
            .flatMap(userId -> {
                if (userId == null) {
                    return Uni.createFrom().failure(new IllegalArgumentException("Invalid API key"));
                }

                // Delete old key
                return redis.key().del(redisKey)
                    .flatMap(deleted -> {
                        // Generate new key
                        String newApiKey = generateApiKey(userId);
                        return Uni.createFrom().item(newApiKey);
                    });
            })
            .onFailure().recoverWithUni(throwable -> {
                Log.errorf(throwable, "Failed to rotate API key");
                return Uni.createFrom().failure(throwable);
            });
    }

    /**
     * Scheduled task to check for expiring API keys.
     */
    @Scheduled(every = "1h")
    void checkExpiringKeys() {
        if (!enabled) {
            return;
        }

        Log.debug("Checking for expiring API keys...");

        // In production, you'd scan Redis for keys approaching expiration
        // and send notifications to users

        Instant warningThreshold = Instant.now().plus(
            config.apiKeys().rotation().warningDays(),
            ChronoUnit.DAYS
        );

        Log.debugf("API key warning threshold: %s", warningThreshold);
    }

    /**
     * Get API key metadata.
     */
    public Uni<ApiKeyMetadata> getApiKeyMetadata(String apiKey) {
        String metadataKey = API_KEY_METADATA_PREFIX + apiKey;

        return redis.value(String.class).get(metadataKey)
            .map(metadata -> {
                String[] parts = metadata.split(":");
                return new ApiKeyMetadata(
                    parts[0],
                    Instant.parse(parts[1]),
                    Instant.parse(parts[2])
                );
            })
            .onFailure().recoverWithItem((ApiKeyMetadata) null);
    }

    private void storeMetadata(String apiKey, String userId, Instant expiresAt) {
        String metadataKey = API_KEY_METADATA_PREFIX + apiKey;
        String metadata = String.format("%s:%s:%s",
            userId,
            Instant.now().toString(),
            expiresAt.toString()
        );

        redis.value(String.class)
            .setex(metadataKey, config.apiKeys().rotation().autoRotateDays() * 86400, metadata)
            .subscribe()
            .with(
                unused -> Log.debugf("Stored API key metadata for %s", apiKey),
                failure -> Log.warnf(failure, "Failed to store API key metadata")
            );
    }

    /**
     * Record for API key metadata.
     */
    public record ApiKeyMetadata(
        String userId,
        Instant createdAt,
        Instant expiresAt
    ) {}
}
