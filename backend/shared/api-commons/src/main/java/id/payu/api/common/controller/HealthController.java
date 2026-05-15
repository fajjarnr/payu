package id.payu.api.common.controller;

import id.payu.api.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.listener.ListenerContainerRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Standard health check controller for all PayU services.
 * Provides common health endpoints with real dependency checks
 * (Redis PING, Kafka listener status) when dependencies are available.
 * Database check is handled by per-service HealthControllers
 * which have direct DataSource access.
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Health check endpoints")
public class HealthController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final RedisConnectionFactory redisConnectionFactory;
    private final ListenerContainerRegistry listenerRegistry;

    public HealthController(
            @Autowired(required = false) RedisConnectionFactory redisConnectionFactory,
            @Autowired(required = false) ListenerContainerRegistry listenerRegistry) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.listenerRegistry = listenerRegistry;
    }

    @GetMapping
    @Operation(summary = "Health check with dependency verification",
               description = "Returns service health status with real dependency checks (Redis PING, Kafka listeners)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> health = new LinkedHashMap<>();
        Map<String, Object> details = new LinkedHashMap<>();

        boolean redisUp = checkRedis(details);
        boolean kafkaUp = checkKafka(details);

        boolean allUp = redisUp && kafkaUp;

        health.put("status", allUp ? "UP" : "DOWN");
        health.put("timestamp", Instant.now().toString());
        health.put("service", getServiceName());
        health.put("details", details);

        return ok(health);
    }

    @GetMapping("/ready")
    @Operation(summary = "Readiness check with real dependency verification",
               description = "Returns if the service is ready to accept traffic (all deps available)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ready() {
        Map<String, Object> readiness = new LinkedHashMap<>();
        Map<String, Object> details = new LinkedHashMap<>();

        boolean redisUp = checkRedis(details);
        boolean kafkaUp = checkKafka(details);

        boolean allReady = redisUp && kafkaUp;

        readiness.put("status", allReady ? "READY" : "NOT_READY");
        readiness.put("timestamp", Instant.now().toString());
        readiness.put("details", details);

        return ok(readiness);
    }

    @GetMapping("/live")
    @Operation(summary = "Liveness check", description = "Returns if the service is alive")
    public ResponseEntity<ApiResponse<Map<String, Object>>> live() {
        Map<String, Object> liveness = Map.of(
                "status", "ALIVE",
                "timestamp", Instant.now().toString()
        );
        return ok(liveness);
    }

    /**
     * Check Redis connectivity with PING command.
     */
    private boolean checkRedis(Map<String, Object> details) {
        if (redisConnectionFactory == null) {
            details.put("redis", "NOT_CONFIGURED");
            return true;
        }
        long start = System.currentTimeMillis();
        try {
            String pong = redisConnectionFactory.getConnection().ping();
            long duration = System.currentTimeMillis() - start;
            if ("PONG".equals(pong)) {
                details.put("redis", "UP");
                details.put("redis.latency_ms", duration);
                return true;
            } else {
                details.put("redis", "DOWN");
                details.put("redis.error", "Unexpected PING response: " + pong);
                details.put("redis.latency_ms", duration);
                return false;
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.warn("Redis health check failed: {}", e.getMessage());
            details.put("redis", "DOWN");
            details.put("redis.error", e.getClass().getSimpleName() + ": " + e.getMessage());
            details.put("redis.latency_ms", duration);
            return false;
        }
    }

    /**
     * Check Kafka listener status.
     */
    private boolean checkKafka(Map<String, Object> details) {
        if (listenerRegistry == null || listenerRegistry.getListenerContainerIds().isEmpty()) {
            details.put("kafka", "NOT_CONFIGURED");
            return true;
        }
        long start = System.currentTimeMillis();
        try {
            boolean allRunning = listenerRegistry.getListenerContainerIds().stream()
                    .allMatch(id -> {
                        var container = listenerRegistry.getListenerContainer(id);
                        return container != null && container.isRunning();
                    });
            long duration = System.currentTimeMillis() - start;
            if (allRunning) {
                details.put("kafka", "UP");
                details.put("kafka.listeners", listenerRegistry.getListenerContainerIds().size());
                details.put("kafka.latency_ms", duration);
                return true;
            } else {
                details.put("kafka", "DOWN");
                details.put("kafka.error", "Some Kafka listeners are not running");
                details.put("kafka.latency_ms", duration);
                return false;
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.warn("Kafka health check failed: {}", e.getMessage());
            details.put("kafka", "DOWN");
            details.put("kafka.error", e.getClass().getSimpleName() + ": " + e.getMessage());
            details.put("kafka.latency_ms", duration);
            return false;
        }
    }

    /**
     * Returns the service name.
     * Subclasses can override this to provide the actual service name.
     */
    protected String getServiceName() {
        return "payu-service";
    }
}
