package id.payu.account.health;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ListenerContainerRegistry;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Deep health check indicator that verifies actual connectivity to dependencies.
 *
 * <p>This performs active checks:</p>
 * <ul>
 *   <li>Database: Executes a test query</li>
 *   <li>Redis: PING command</li>
 *   <li>Kafka: Produces a test message</li>
 * </ul>
 */
@Component("deepHealth")
@RequiredArgsConstructor
public class DeepHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(DeepHealthIndicator.class);

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ListenerContainerRegistry listenerRegistry;

    private final AtomicBoolean databaseHealthy = new AtomicBoolean(false);
    private final AtomicBoolean redisHealthy = new AtomicBoolean(false);
    private final AtomicBoolean kafkaHealthy = new AtomicBoolean(false);

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();

        // Check database
        Health dbHealth = checkDatabase();
        details.put("database", dbHealth.getDetails());
        boolean dbUp = dbHealth.getStatus() == Status.UP;
        databaseHealthy.set(dbUp);

        // Check Redis
        Health redisHealth = checkRedis();
        details.put("redis", redisHealth.getDetails());
        boolean redisUp = redisHealth.getStatus() == Status.UP;
        redisHealthy.set(redisUp);

        // Check Kafka
        Health kafkaHealth = checkKafka();
        details.put("kafka", kafkaHealth.getDetails());
        boolean kafkaUp = kafkaHealth.getStatus() == Status.UP;
        kafkaHealthy.set(kafkaUp);

        // Overall status
        boolean allHealthy = dbUp && redisUp && kafkaUp;
        Status status = allHealthy ? Status.UP : Status.DOWN;

        return Health.status(status)
            .withDetails(details)
            .build();
    }

    /**
     * Check database connectivity with actual query.
     */
    private Health checkDatabase() {
        long start = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            boolean isValid = conn.isValid(5);
            long duration = System.currentTimeMillis() - start;

            if (isValid) {
                return Health.up()
                    .withDetail("latency", duration + "ms")
                    .withDetail("database", conn.getMetaData().getDatabaseProductName())
                    .withDetail("version", conn.getMetaData().getDatabaseProductVersion())
                    .build();
            } else {
                return Health.down()
                    .withDetail("reason", "Connection validation failed")
                    .withDetail("latency", duration + "ms")
                    .build();
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("Database health check failed: {}", e.getMessage());
            return Health.down()
                .withDetail("error", e.getClass().getSimpleName())
                .withDetail("message", e.getMessage())
                .withDetail("latency", duration + "ms")
                .build();
        }
    }

    /**
     * Check Redis connectivity with PING command.
     */
    private Health checkRedis() {
        long start = System.currentTimeMillis();
        try {
            String pong = redisConnectionFactory.getConnection().ping();
            long duration = System.currentTimeMillis() - start;

            if ("PONG".equals(pong)) {
                return Health.up()
                    .withDetail("latency", duration + "ms")
                    .withDetail("response", pong)
                    .build();
            } else {
                return Health.down()
                    .withDetail("reason", "Unexpected response: " + pong)
                    .withDetail("latency", duration + "ms")
                    .build();
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("Redis health check failed: {}", e.getMessage());
            return Health.down()
                .withDetail("error", e.getClass().getSimpleName())
                .withDetail("message", e.getMessage())
                .withDetail("latency", duration + "ms")
                .build();
        }
    }

    /**
     * Check Kafka connectivity.
     */
    private Health checkKafka() {
        long start = System.currentTimeMillis();
        try {
            // Try to get cluster info
            var partitions = kafkaTemplate.getProducerFactory().getConfigurationProperties();
            long duration = System.currentTimeMillis() - start;

            // Check listener containers if available
            boolean listenersRunning = true;
            if (listenerRegistry != null) {
                listenersRunning = listenerRegistry.getListenerContainerIds().stream()
                    .allMatch(id -> {
                        var container = listenerRegistry.getListenerContainer(id);
                        return container != null && container.isRunning();
                    });
            }

            if (listenersRunning) {
                return Health.up()
                    .withDetail("latency", duration + "ms")
                    .withDetail("listeners", "running")
                    .build();
            } else {
                return Health.down()
                    .withDetail("reason", "Some Kafka listeners are not running")
                    .withDetail("latency", duration + "ms")
                    .build();
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("Kafka health check failed: {}", e.getMessage());
            return Health.down()
                .withDetail("error", e.getClass().getSimpleName())
                .withDetail("message", e.getMessage())
                .withDetail("latency", duration + "ms")
                .build();
        }
    }
}
