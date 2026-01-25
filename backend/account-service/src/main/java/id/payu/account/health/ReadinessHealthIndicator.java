package id.payu.account.health;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.kafka.listener.ListenerContainerRegistry;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Readiness probe - checks if the application is ready to serve traffic.
 *
 * <p>This checks if all critical dependencies are available:</p>
 * <ul>
 *   <li>Database: Can establish connection</li>
 *   <li>Redis: Can establish connection</li>
 *   <li>Kafka: Listeners are running</li>
 * </ul>
 */
@Component("customReadiness")
@ConditionalOnBean(DataSource.class)
@RequiredArgsConstructor
public class ReadinessHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(ReadinessHealthIndicator.class);

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;
    private final ListenerContainerRegistry listenerRegistry;

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();

        // Check database - only connection, no query
        boolean dbReady = isDatabaseReady(details);

        // Check Redis - only connection, no ping
        boolean redisReady = isRedisReady(details);

        // Check Kafka - listeners running
        boolean kafkaReady = isKafkaReady(details);

        // Overall status
        boolean allReady = dbReady && redisReady && kafkaReady;
        Status status = allReady ? Status.UP : Status.OUT_OF_SERVICE;

        return Health.status(status)
            .withDetails(details)
            .build();
    }

    /**
     * Check if database is ready (can establish connection).
     */
    private boolean isDatabaseReady(Map<String, Object> details) {
        long start = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            long duration = System.currentTimeMillis() - start;
            details.put("database", "UP");
            details.put("database.latency", duration + "ms");
            return true;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.warn("Database readiness check failed: {}", e.getMessage());
            details.put("database", "DOWN");
            details.put("database.error", e.getClass().getSimpleName());
            details.put("database.latency", duration + "ms");
            return false;
        }
    }

    /**
     * Check if Redis is ready (can establish connection).
     */
    private boolean isRedisReady(Map<String, Object> details) {
        long start = System.currentTimeMillis();
        try {
            redisConnectionFactory.getConnection().close();
            long duration = System.currentTimeMillis() - start;
            details.put("redis", "UP");
            details.put("redis.latency", duration + "ms");
            return true;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.warn("Redis readiness check failed: {}", e.getMessage());
            details.put("redis", "DOWN");
            details.put("redis.error", e.getClass().getSimpleName());
            details.put("redis.latency", duration + "ms");
            return false;
        }
    }

    /**
     * Check if Kafka is ready (listeners are running).
     */
    private boolean isKafkaReady(Map<String, Object> details) {
        long start = System.currentTimeMillis();
        try {
            if (listenerRegistry == null || listenerRegistry.getListenerContainerIds().isEmpty()) {
                // No Kafka listeners configured
                details.put("kafka", "NOT_CONFIGURED");
                return true;
            }

            boolean allRunning = listenerRegistry.getListenerContainerIds().stream()
                .allMatch(id -> {
                    var container = listenerRegistry.getListenerContainer(id);
                    return container != null && container.isRunning();
                });

            long duration = System.currentTimeMillis() - start;

            if (allRunning) {
                details.put("kafka", "UP");
                details.put("kafka.listeners", listenerRegistry.getListenerContainerIds().size());
                details.put("kafka.latency", duration + "ms");
                return true;
            } else {
                details.put("kafka", "STARTING");
                details.put("kafka.latency", duration + "ms");
                return false;
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.warn("Kafka readiness check failed: {}", e.getMessage());
            details.put("kafka", "DOWN");
            details.put("kafka.error", e.getClass().getSimpleName());
            details.put("kafka.latency", duration + "ms");
            return false;
        }
    }
}
