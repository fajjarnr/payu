package id.payu.gateway.adapter.cache;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.marshall.UTF8StringMarshaller;

/**
 * Reactive facade for the gateway's native Infinispan Hot Rod cache.
 */
@ApplicationScoped
public class HotRodCacheClient {

    @ConfigProperty(name = "payu.cache.hotrod.server-list", defaultValue = "localhost:11222")
    String serverList;

    @ConfigProperty(name = "payu.cache.hotrod.cache-name", defaultValue = "payu")
    String cacheName;

    @ConfigProperty(name = "payu.cache.hotrod.auth-username")
    Optional<String> username;

    @ConfigProperty(name = "payu.cache.hotrod.auth-password")
    Optional<String> password;

    @ConfigProperty(name = "payu.cache.hotrod.auth-realm", defaultValue = "default")
    String realm;

    @ConfigProperty(name = "payu.cache.hotrod.sasl-mechanism", defaultValue = "DIGEST-MD5")
    String saslMechanism;

    private RemoteCacheManager cacheManager;
    private RemoteCache<String, String> cache;

    @PostConstruct
    void init() {
        ConfigurationBuilder builder = new ConfigurationBuilder()
                .addServers(serverList)
                .marshaller(UTF8StringMarshaller.class)
                .clientIntelligence(ClientIntelligence.HASH_DISTRIBUTION_AWARE);
        if (username.isPresent()) {
            builder.security().authentication()
                    .username(username.get())
                    .password(password.orElseThrow(() -> new IllegalStateException("Hot Rod password is required")))
                    .realm(realm)
                    .saslMechanism(saslMechanism);
        }
        cacheManager = new RemoteCacheManager(builder.build(), false);
        Log.infof("Gateway Hot Rod client configured for cache %s at %s", cacheName, serverList);
    }

    public Uni<String> get(String key) {
        return Uni.createFrom().completionStage(() -> remoteCache().getAsync(key));
    }

    public Uni<Void> put(String key, String value, Duration ttl) {
        return Uni.createFrom().completionStage(
                        () -> remoteCache().putAsync(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS))
                .replaceWithVoid();
    }

    public Uni<Void> remove(String key) {
        return Uni.createFrom().completionStage(() -> remoteCache().removeAsync(key)).replaceWithVoid();
    }

    public Uni<Long> increment(String key, Duration ttl) {
        return Uni.createFrom().item(() -> incrementSynchronously(key, ttl))
                .runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultExecutor());
    }

    /**
     * Atomically records one request and returns the active sliding-window state.
     */
    public Uni<SlidingWindow> recordSlidingWindowRequest(String key, Duration window) {
        return Uni.createFrom().item(() -> recordSlidingWindowRequestSynchronously(key, window))
                .runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultExecutor());
    }

    public Uni<Void> appendToList(String key, String value, Duration ttl) {
        return Uni.createFrom().item(() -> {
                    appendToListSynchronously(key, value, ttl);
                    return true;
                })
                .runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultExecutor())
                .replaceWithVoid();
    }

    public Uni<List<String>> readList(String key) {
        return get(key).map(this::decodeList);
    }

    private RemoteCache<String, String> remoteCache() {
        if (!cacheManager.isStarted()) {
            cacheManager.start();
        }
        if (cache == null) {
            cache = cacheManager.getCache(cacheName);
        }
        return cache;
    }

    private long incrementSynchronously(String key, Duration ttl) {
        RemoteCache<String, String> remoteCache = remoteCache();
        for (int attempt = 0; attempt < 8; attempt++) {
            var current = remoteCache.getWithMetadata(key);
            if (current == null) {
                if (remoteCache.putIfAbsent(key, "1", ttl.toMillis(), TimeUnit.MILLISECONDS) == null) {
                    return 1L;
                }
                continue;
            }
            long next = Long.parseLong(current.getValue()) + 1;
            if (remoteCache.replaceWithVersion(key, Long.toString(next), current.getVersion())) {
                return next;
            }
        }
        throw new IllegalStateException("Could not atomically increment cache key: " + key);
    }

    private SlidingWindow recordSlidingWindowRequestSynchronously(String key, Duration window) {
        RemoteCache<String, String> remoteCache = remoteCache();
        long now = System.currentTimeMillis();
        long oldestAllowed = now - window.toMillis();
        for (int attempt = 0; attempt < 32; attempt++) {
            var current = remoteCache.getWithMetadata(key);
            List<Long> timestamps = current == null ? new ArrayList<>() : timestampsAfter(current.getValue(), oldestAllowed);
            timestamps.add(now);
            String value = timestamps.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
            if (current == null) {
                if (remoteCache.putIfAbsent(key, value, window.multipliedBy(2).toMillis(), TimeUnit.MILLISECONDS) == null) {
                    return new SlidingWindow(timestamps.size(), timestamps.getFirst());
                }
            } else if (remoteCache.replaceWithVersion(key, value, current.getVersion())) {
                return new SlidingWindow(timestamps.size(), timestamps.getFirst());
            }
        }
        throw new IllegalStateException("Could not atomically update sliding-window key: " + key);
    }

    private List<Long> timestampsAfter(String value, long oldestAllowed) {
        List<Long> timestamps = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return timestamps;
        }
        for (String timestamp : value.split(",")) {
            try {
                long parsed = Long.parseLong(timestamp);
                if (parsed > oldestAllowed) {
                    timestamps.add(parsed);
                }
            } catch (NumberFormatException ignored) {
                Log.warnf("Discarding malformed rate-limit timestamp for cache key");
            }
        }
        return timestamps;
    }

    private void appendToListSynchronously(String key, String value, Duration ttl) {
        RemoteCache<String, String> remoteCache = remoteCache();
        String encoded = Base64.getEncoder().encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        for (int attempt = 0; attempt < 32; attempt++) {
            var current = remoteCache.getWithMetadata(key);
            String updated = current == null || current.getValue().isEmpty() ? encoded : current.getValue() + "\n" + encoded;
            if (current == null) {
                if (remoteCache.putIfAbsent(key, updated, ttl.toMillis(), TimeUnit.MILLISECONDS) == null) {
                    return;
                }
            } else if (remoteCache.replaceWithVersion(key, updated, current.getVersion())) {
                return;
            }
        }
        throw new IllegalStateException("Could not atomically append cache list: " + key);
    }

    private List<String> decodeList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> decoded = new ArrayList<>();
        for (String encoded : value.split("\\n")) {
            try {
                decoded.add(new String(Base64.getDecoder().decode(encoded), java.nio.charset.StandardCharsets.UTF_8));
            } catch (IllegalArgumentException ignored) {
                Log.warn("Discarding malformed cache list item");
            }
        }
        return List.copyOf(decoded);
    }

    @PreDestroy
    void stop() {
        if (cacheManager != null) {
            cacheManager.stop();
        }
    }

    public record SlidingWindow(long count, long oldestRequestEpochMillis) {}
}
