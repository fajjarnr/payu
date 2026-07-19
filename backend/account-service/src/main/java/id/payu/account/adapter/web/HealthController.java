package id.payu.account.adapter.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.listener.ListenerContainerRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check controller that verifies real connectivity to service dependencies.
 * Checks database (SELECT 1), Redis (PING), and Kafka (listener status).
 *
 * <p>Returns UP only if all critical dependencies are available.</p>
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);
    private static final String SERVICE_NAME = "account-service";

    private final DataSource dataSource;
    private final RemoteCacheManager remoteCacheManager;
    private final ListenerContainerRegistry listenerRegistry;

    public HealthController(DataSource dataSource,
                           @Autowired(required = false) RemoteCacheManager remoteCacheManager,
                           @Autowired(required = false) ListenerContainerRegistry listenerRegistry) {
        this.dataSource = dataSource;
        this.remoteCacheManager = remoteCacheManager;
        this.listenerRegistry = listenerRegistry;
    }

    @GetMapping("/public/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> details = new LinkedHashMap<>();

        boolean dbUp = checkDatabase(details);
        boolean dataGridUp = checkDataGrid(details);
        boolean kafkaUp = checkKafka(details);

        boolean allUp = dbUp && dataGridUp && kafkaUp;

        result.put("status", allUp ? "UP" : "DOWN");
        result.put("service", SERVICE_NAME);
        result.put("timestamp", Instant.now().toString());
        result.put("details", details);

        return ResponseEntity.ok(result);
    }

    private boolean checkDatabase(Map<String, Object> details) {
        long start = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
            long duration = System.currentTimeMillis() - start;
            details.put("database", "UP");
            details.put("database.latency_ms", duration);
            return true;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.warn("Database health check failed: {}", e.getMessage());
            details.put("database", "DOWN");
            details.put("database.error", e.getClass().getSimpleName() + ": " + e.getMessage());
            details.put("database.latency_ms", duration);
            return false;
        }
    }

    private boolean checkDataGrid(Map<String, Object> details) {
        if (remoteCacheManager == null) {
            details.put("datagrid", "NOT_CONFIGURED");
            return true;
        }
        long start = System.currentTimeMillis();
        try {
            remoteCacheManager.getCache().containsKey("__payu_health__");
            long duration = System.currentTimeMillis() - start;
            details.put("datagrid", "UP");
            details.put("datagrid.latency_ms", duration);
            return true;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.warn("Data Grid health check failed: {}", e.getMessage());
            details.put("datagrid", "DOWN");
            details.put("datagrid.error", e.getClass().getSimpleName() + ": " + e.getMessage());
            details.put("datagrid.latency_ms", duration);
            return false;
        }
    }

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
}
