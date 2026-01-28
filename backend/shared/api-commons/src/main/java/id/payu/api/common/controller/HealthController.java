package id.payu.api.common.controller;

import id.payu.api.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Standard health check controller for all PayU services.
 * Provides common health endpoints for monitoring and service discovery.
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Health check endpoints")
public class HealthController extends BaseController {

    @GetMapping
    @Operation(summary = "Basic health check", description = "Returns if the service is running")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "timestamp", Instant.now(),
                "service", getServiceName()
        );
        return ok(health);
    }

    @GetMapping("/ready")
    @Operation(summary = "Readiness check", description = "Returns if the service is ready to accept traffic")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ready() {
        Map<String, Object> readiness = Map.of(
                "status", "READY",
                "timestamp", Instant.now()
        );
        return ok(readiness);
    }

    @GetMapping("/live")
    @Operation(summary = "Liveness check", description = "Returns if the service is alive")
    public ResponseEntity<ApiResponse<Map<String, Object>>> live() {
        Map<String, Object> liveness = Map.of(
                "status", "ALIVE",
                "timestamp", Instant.now()
        );
        return ok(liveness);
    }

    /**
     * Returns the service name.
     * Subclasses can override this to provide the actual service name.
     */
    protected String getServiceName() {
        return "payu-service";
    }
}
