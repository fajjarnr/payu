package id.payu.account.health;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
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

        // Extract individual dependency status. DeepHealthIndicator stores each
        // dependency's Health in its own details map (Health.down()/.up() are
        // the values), so read the status via a fresh Health check is wrong —
        // deepHealth.getDetails() returns Map<String, Object> with the Health
        // objects directly as values.
        var deepDetails = deepHealth.getDetails();
        for (String dep : java.util.List.of("database", "redis", "kafka")) {
            if (deepDetails.get(dep) instanceof Health depHealth) {
                dependencySummary.put(dep, depHealth.getStatus().toString());
            }
        }

        details.put("dependencies", dependencySummary);

        // Overall status based on deep health
        return Health.status(deepHealth.getStatus())
            .withDetails(details)
            .build();
    }
}
