package id.payu.promotion.adapter.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
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
 * Checks database (SELECT 1).
 */
@RestController
@RequestMapping("/api/v1/promotions")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);
    private static final String SERVICE_NAME = "promotion-service";

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/public/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> details = new LinkedHashMap<>();

        boolean dbUp = checkDatabase(details);

        result.put("status", dbUp ? "UP" : "DOWN");
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
}
