package id.payu.account.health;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.availability.LivenessState;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Dependency health indicator that reports health of all dependencies.
 *
 * <p>This provides a consolidated view of all dependencies including:</p>
 * <ul>
 *   <li>Database connectivity and latency</li>
 *   <li>Redis connectivity and latency</li>
 *   <li>Kafka connectivity and listener status</li>
 *   <li>External service health</li>
 * </ul>
 */
@Component("dependencies")
@RequiredArgsConstructor
public class DependencyHealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(DependencyHealthIndicator.class);

    private final ApplicationAvailability availability;
    private final DeepHealthIndicator deepHealthIndicator;

    public Health health() {
        Map<String, Object> details = new HashMap<>();

        // Get liveness and readiness state
        LivenessState livenessState = availability.getLivenessState();
        ReadinessState readinessState = availability.getReadinessState();

        details.put("liveness", livenessState.toString());
        details.put("readiness", readinessState.toString());

        // Get deep health status
        Health deepHealth = deepHealthIndicator.health();
        details.put("deepHealth", deepHealth.getStatus().toString());

        // Dependency health summary
        Map<String, String> dependencySummary = new HashMap<>();

        if (deepHealth.getStatus() == Status.UP) {
            dependencySummary.put("overall", "HEALTHY");
        } else {
            dependencySummary.put("overall", "UNHEALTHY");
        }

        // Extract individual dependency status
        var deepDetails = deepHealth.getDetails();
        if (deepDetails.containsKey("database")) {
            var dbHealth = (Health) deepDetails.get("database");
            dependencySummary.put("database", dbHealth.getStatus().toString());
        }
        if (deepDetails.containsKey("redis")) {
            var redisHealth = (Health) deepDetails.get("redis");
            dependencySummary.put("redis", redisHealth.getStatus().toString());
        }
        if (deepDetails.containsKey("kafka")) {
            var kafkaHealth = (Health) deepDetails.get("kafka");
            dependencySummary.put("kafka", kafkaHealth.getStatus().toString());
        }

        details.put("dependencies", dependencySummary);

        // Overall status based on deep health
        return Health.status(deepHealth.getStatus())
            .withDetails(details)
            .build();
    }
}
